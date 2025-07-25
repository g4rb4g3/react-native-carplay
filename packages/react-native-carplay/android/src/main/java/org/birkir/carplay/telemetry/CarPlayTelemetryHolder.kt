package org.birkir.carplay.telemetry

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap

class CarPlayTelemetryHolder {
  private var isDirty = false
  private val lock = Any()

  private var batteryLevel: Float? = null
  private var batteryLevelTimestamp = 0

  private var fuelLevel: Float? = null
  private var fuelLevelTimestamp = 0

  private var range: Float? = null
  private var rangeTimestamp = 0

  private var speed: Float? = null
  private var speedTimestamp = 0

  private var odometer: Float? = null
  private var odometerTimestamp = 0

  fun updateBatteryLevel(value: Float?) = synchronized(lock) {
    batteryLevel = value
    batteryLevelTimestamp = (System.currentTimeMillis() / 1000L).toInt()
    isDirty = true
  }

  fun updateFuelLevel(value: Float?) = synchronized(lock) {
    fuelLevel = value
    fuelLevelTimestamp = (System.currentTimeMillis() / 1000L).toInt()
    isDirty = true
  }

  fun updateRange(value: Float?) = synchronized(lock) {
    range = value
    rangeTimestamp = (System.currentTimeMillis() / 1000L).toInt()
    isDirty = true
  }

  fun updateSpeed(value: Float?) = synchronized(lock) {
    speed = value
    speedTimestamp = (System.currentTimeMillis() / 1000L).toInt()
    isDirty = true
  }

  fun updateOdometer(value: Float?) = synchronized(lock) {
    odometer = value
    odometerTimestamp = (System.currentTimeMillis() / 1000L).toInt()
    isDirty = true
  }

  fun toMap(): WritableMap? {
    synchronized(lock) {
      if (!isDirty) {
        return null
      }
      
      isDirty = false

      return Arguments.createMap().apply {
        createPropertyMap(batteryLevel, batteryLevelTimestamp)?.let {
          putMap("batteryLevel", it)
        }
        createPropertyMap(fuelLevel, fuelLevelTimestamp)?.let {
          putMap("fuelLevel", it)
        }
        createPropertyMap(range, rangeTimestamp)?.let {
          putMap("range", it)
        }
        createPropertyMap(speed, speedTimestamp)?.let {
          putMap("speed", it)
        }
        createPropertyMap(odometer, odometerTimestamp)?.let {
          putMap("odometer", it)
        }
      }
    }
  }

  private fun createPropertyMap(value: Float?, timestamp: Int): WritableMap? {
    if (value == null && timestamp == 0) {
      return null
    }

    return Arguments.createMap().apply {
      putInt("timestamp", timestamp)
      value?.let {
        putDouble("value", it.toDouble())
      } ?: run {
        putNull("value")
      }
    }
  }
}