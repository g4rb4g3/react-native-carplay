package org.birkir.carplay.parser

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarColor
import androidx.car.app.model.LongMessageTemplate
import androidx.car.app.model.ParkedOnlyOnClickListener
import androidx.car.app.model.Template
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import org.birkir.carplay.utils.AppInfo

class RCTPermissionRequestTemplate(
  private val carContext: CarContext,
  private val message: String,
  private val actionTitle: String,
  private val actionColor: CarColor,
  private val permissions: List<String>,
  private val promise: Promise
) : Screen(carContext) {
  private val appName = AppInfo.getApplicationLabel(carContext)

  override fun onGetTemplate(): Template {
    return LongMessageTemplate.Builder(message).setTitle(appName).setHeaderAction(Action.APP_ICON)
      .addAction(
        Action.Builder().setTitle(actionTitle).setBackgroundColor(actionColor)
          .setOnClickListener(ParkedOnlyOnClickListener.create {
            carContext.requestPermissions(
              permissions
            ) { granted: List<String>, denied: List<String> ->
              promise.resolve(Arguments.createMap().apply {
                putArray("denied",
                  Arguments.createArray().apply { denied.forEach { pushString(it) } })
                putArray("granted",
                  Arguments.createArray().apply { granted.forEach { pushString(it) } })
              })
              finish()
            }
          }).build()
      ).build()
  }

  companion object {
    const val TAG = "RCTPermissionRequestTemplate"
  }
}