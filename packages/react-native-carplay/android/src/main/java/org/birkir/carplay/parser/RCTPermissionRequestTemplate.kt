package org.birkir.carplay.parser

import androidx.activity.OnBackPressedCallback
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.LongMessageTemplate
import androidx.car.app.model.ParkedOnlyOnClickListener
import androidx.car.app.model.Template
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
  }

  override fun onGetTemplate(): Template {
    return LongMessageTemplate.Builder(message)
      .setTitle(appName)
      .setHeaderAction(headerAction)
      .addAction(primaryAction)
      .build()
  }

  companion object {
    const val TAG = "RCTPermissionRequestTemplate"
  }
}