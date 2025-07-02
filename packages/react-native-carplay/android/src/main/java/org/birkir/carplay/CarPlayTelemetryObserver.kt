package org.birkir.carplay

import android.os.Handler
import android.os.Looper
import androidx.car.app.CarContext
import androidx.car.app.hardware.CarHardwareManager
import androidx.car.app.hardware.common.CarValue
import androidx.car.app.hardware.common.OnCarDataAvailableListener
import androidx.car.app.hardware.info.EnergyLevel
import androidx.car.app.hardware.info.Mileage
import androidx.car.app.hardware.info.Model
import androidx.car.app.hardware.info.Speed
import androidx.car.app.versioning.CarAppApiLevels
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import org.birkir.carplay.utils.EventEmitter

object CarPlayTelemetryObserver {
  private var isRunning = false
  private var carContext: CarContext? = null
  private var eventEmitter: EventEmitter? = null

  private val telemetry = Arguments.createMap()
  private var isTelemetryDirty = false
  private val handler = Handler(Looper.getMainLooper())

  private val mModelListener = OnCarDataAvailableListener<Model> {
    val vehicle = Arguments.createMap()
    if (it.name.status == CarValue.STATUS_SUCCESS) {
      vehicle.putMap("name", Arguments.createMap().apply {
        putString("value", it.name.value)
        putDouble("timestamp", it.name.timestampMillis / 1000.0)
      })
    }
    if (it.manufacturer.status == CarValue.STATUS_SUCCESS) {
      vehicle.putMap("manufacturer", Arguments.createMap().apply {
        putString("value", it.manufacturer.value)
        putDouble("timestamp", it.manufacturer.timestampMillis / 1000.0)
      })
    }
    if (it.year.status == CarValue.STATUS_SUCCESS) {
      it.year.value?.let { year ->
        vehicle.putMap("year", Arguments.createMap().apply {
          putInt("value", year)
          putDouble("timestamp", it.year.timestampMillis / 1000.0)
        })
      } ?: run {
        vehicle.putMap("year", Arguments.createMap().apply {
          putNull("value")
          putDouble("timestamp", it.year.timestampMillis / 1000.0)
        })
      }
    }
    eventEmitter?.telemetry(Arguments.createMap().apply {
      putMap("vehicle", vehicle)
    })
  }

  private val mEnergyLevelListener = OnCarDataAvailableListener<EnergyLevel> { carEnergyLevel ->
    synchronized(
      telemetry
    ) {
      val timestamp = System.currentTimeMillis() / 1000.0

      if (carEnergyLevel.batteryPercent.status == CarValue.STATUS_SUCCESS) {
        carEnergyLevel.batteryPercent.value?.let {
          telemetry.putMap("batteryLevel", Arguments.createMap().apply {
            putDouble("value", it.toDouble())
            putDouble("timestamp", timestamp)
          })
        } ?: run {
          telemetry.putMap("batteryLevel", Arguments.createMap().apply {
            putNull("value")
            putDouble("timestamp", timestamp)
          })
        }

        isTelemetryDirty = true
      }
      if (carEnergyLevel.fuelPercent.status == CarValue.STATUS_SUCCESS) {
        carEnergyLevel.fuelPercent.value?.let {
          telemetry.putMap("fuelLevel", Arguments.createMap().apply {
            putDouble("value", it.toDouble())
            putDouble("timestamp", timestamp)
          })
        } ?: run {
          telemetry.putMap("fuelLevel", Arguments.createMap().apply {
            putNull("value")
            putDouble("timestamp", timestamp)
          })
        }

        isTelemetryDirty = true
      }
      if (carEnergyLevel.rangeRemainingMeters.status == CarValue.STATUS_SUCCESS) {
        carEnergyLevel.rangeRemainingMeters.value?.let {
          telemetry.putMap("range", Arguments.createMap().apply {
            putDouble("value", it.div(1000.0))
            putDouble("timestamp", timestamp)
          })
        } ?: run {
          telemetry.putMap("range", Arguments.createMap().apply {
            putNull("value")
            putDouble("timestamp", timestamp)
          })
        }

        isTelemetryDirty = true
      }
    }
  }

  private val mSpeedListener = OnCarDataAvailableListener<Speed> { carSpeed ->
    synchronized(
      telemetry
    ) {
      if (carSpeed.displaySpeedMetersPerSecond.status == CarValue.STATUS_SUCCESS) {
        val timestamp = System.currentTimeMillis() / 1000.0
        carSpeed.displaySpeedMetersPerSecond.value?.let {
          // convert to km/h
          telemetry.putMap("odometer", Arguments.createMap().apply {
            putDouble("value", it.times(3.6))
            putDouble("timestamp", timestamp)
          })
        } ?: run {
          telemetry.putMap("speed", Arguments.createMap().apply {
            putNull("value")
            putDouble("timestamp", timestamp)
          })
        }

        isTelemetryDirty = true
      }
    }
  }

  private val mMileageListener = OnCarDataAvailableListener<Mileage> { carMileage ->
    synchronized(
      telemetry
    ) {
      if (carMileage.odometerMeters.status == CarValue.STATUS_SUCCESS) {
        val timestamp = System.currentTimeMillis() / 1000.0
        carMileage.odometerMeters.value?.let {
          telemetry.putMap("odometer", Arguments.createMap().apply {
            putDouble("value", it.div(1000.0))
            putDouble("timestamp", timestamp)
          })
        } ?: run {
          telemetry.putMap("odometer", Arguments.createMap().apply {
            putNull("value")
            putDouble("timestamp", timestamp)
          })
        }

        isTelemetryDirty = true
      }
    }
  }

  private val emitter = object : Runnable {
    override fun run() {
      if (isTelemetryDirty) {
        synchronized(telemetry) {
          eventEmitter?.telemetry(Arguments.createMap().apply { merge(telemetry) })
          isTelemetryDirty = false
        }
      }

      handler.postDelayed(this, BuildConfig.CARPLAY_TELEMETRY_INTERVAL_MS)
    }
  }

  fun startTelemetryObserver(
    carContext: CarContext, eventEmitter: EventEmitter, promise: Promise
  ) {
    this.carContext = carContext
    this.eventEmitter = eventEmitter

    if (carContext.carAppApiLevel < CarAppApiLevels.LEVEL_3) {
      promise.reject(UnsupportedOperationException("Telemetry not supported for this API level ${carContext.carAppApiLevel}"))
      return
    }

    val carHardwareExecutor = ContextCompat.getMainExecutor(carContext)

    val carHardwareManager = carContext.getCarService(
      CarHardwareManager::class.java
    )
    val carInfo = carHardwareManager.carInfo

    // Request any single shot values.
    try {
      carInfo.fetchModel(carHardwareExecutor, mModelListener)
    } catch (_: SecurityException) {
    }

    if (isRunning) {
      // we stop here to not re-register multiple listeners, only the single shot values can be requested multiple times by registering another tlm listener on RN side
      promise.resolve("Telemetry observer is already running")
      return
    }

    try {
      carInfo.addEnergyLevelListener(carHardwareExecutor, mEnergyLevelListener)
    } catch (_: SecurityException) {
    }

    try {
      carInfo.addSpeedListener(carHardwareExecutor, mSpeedListener)
    } catch (_: SecurityException) {
    }

    try {
      carInfo.addMileageListener(carHardwareExecutor, mMileageListener)
    } catch (_: SecurityException) {
    }

    handler.post(emitter)

    isRunning = true

    promise.resolve("Telemetry observer started")
  }

  fun stopTelemetryObserver() {
    carContext?.let {
      val carHardwareManager = it.getCarService(
        CarHardwareManager::class.java
      )
      val carInfo = carHardwareManager.carInfo

      try {
        carInfo.removeEnergyLevelListener(mEnergyLevelListener)
      } catch (_: SecurityException) {
      }

      try {
        carInfo.removeSpeedListener(mSpeedListener)
      } catch (_: SecurityException) {
      }

      try {
        carInfo.removeMileageListener(mMileageListener)
      } catch (_: SecurityException) {
      }
    }

    handler.removeCallbacks(emitter)

    isRunning = false
  }
}
