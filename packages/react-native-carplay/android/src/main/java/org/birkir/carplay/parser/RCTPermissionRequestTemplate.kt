package org.birkir.carplay.parser

import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.activity.OnBackPressedCallback
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Header
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.ParkedOnlyOnClickListener
import androidx.car.app.model.Template
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReadableMap
import org.birkir.carplay.utils.AppInfo

class RCTPermissionRequestTemplate(
  private val carContext: CarContext,
  private val permissions: List<String>,
  private val message: String,
  primaryActionConfig: ReadableMap,
  headerActionConfig: ReadableMap,
  private val promise: Promise
) : Screen(carContext) {
  private val appName = AppInfo.getApplicationLabel(carContext)

  private val onPrimaryClick = ParkedOnlyOnClickListener.create {
    carContext.requestPermissions(
      permissions
    ) { granted: List<String>, denied: List<String> ->
      promise.resolve(Arguments.createMap().apply {
        putArray("denied",
          Arguments.createArray().apply { denied.forEach { pushString(it) } })
        putArray("granted",
          Arguments.createArray().apply { granted.forEach { pushString(it) } })
      })
      screenManager.pop()
    }
  }

  private val primaryAction = Parser.parseAction(primaryActionConfig, carContext, onPrimaryClick)
  private val headerAction = Parser.parseAction(headerActionConfig, carContext, null)

  init {
    carContext.onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        promise.resolve(null)
        screenManager.pop()
      }
    })

    var destroyed = false

    val handler = Handler(Looper.getMainLooper())
    val permissionChecker = object : Runnable {
      // this runnable makes sure we catch granted permissions from other templates or react-native PermissionAndroid.requestMultiple
      override fun run() {
        val granted = permissions.all {
          ContextCompat.checkSelfPermission(carContext, it) == PackageManager.PERMISSION_GRANTED
        }
        if (granted) {
          promise.resolve(Arguments.createMap().apply {
            putArray("denied", Arguments.createArray())
            putArray("granted", Arguments.createArray().apply {
              permissions.forEach {
                pushString(it)
              }
            })
          })
          screenManager.pop()
          return
        }

        if (destroyed) {
          return
        }

        handler.postDelayed(this, 1000)
      }
    }

    lifecycle.addObserver(object: LifecycleEventObserver{
      override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
          Lifecycle.Event.ON_CREATE -> {
            handler.postDelayed(permissionChecker, 1000)
          }
          Lifecycle.Event.ON_DESTROY -> {
            destroyed = true
          }
          else -> {}
        }
      }
    })
  }

  override fun onGetTemplate(): Template {
    return MessageTemplate.Builder(message)
      .setHeader(
        Header.Builder().apply {
          setTitle(appName)
          setStartHeaderAction(headerAction)
        }.build())
      .addAction(primaryAction)
      .build()
  }

  companion object {
    const val TAG = "RCTPermissionRequestTemplate"
  }
}