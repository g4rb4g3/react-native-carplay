package org.birkir.carplay.screens

import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import org.birkir.carplay.utils.AppInfo
import org.birkir.carplay.utils.EventEmitter
import org.birkir.carplay.utils.VirtualRenderer

class CarScreen(
  carContext: CarContext, emitter: EventEmitter?, private val isCluster: Boolean = false
) : Screen(carContext) {

  var template: Template? = null
  private var virtualRenderer: VirtualRenderer? = null

  init {
    lifecycle.addObserver(object : LifecycleEventObserver {
      override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
          Lifecycle.Event.ON_CREATE -> {
            emitter?.willAppear()
          }

          Lifecycle.Event.ON_RESUME -> {
            emitter?.didAppear()
          }

          Lifecycle.Event.ON_PAUSE -> {
            emitter?.willDisappear()
          }

          Lifecycle.Event.ON_DESTROY -> {
            emitter?.didDisappear()
          }

          else -> {}
        }

        if (event == Lifecycle.Event.ON_DESTROY && virtualRenderer != null) {
          Log.d(TAG, "onStateChanged: got $event, removing virtual renderer")
          virtualRenderer = null
        }
      }
    })
  }

  fun setTemplate(template: Template, invalidate: Boolean = false, isSurfaceTemplate: Boolean, sessionLifecycle: Lifecycle) {
    if (isSurfaceTemplate && virtualRenderer == null) {
      Log.d(TAG, "firing up virtual renderer for $marker")
      virtualRenderer = VirtualRenderer(carContext, marker!!, isCluster, sessionLifecycle)
    }
    this.template = template

    if (invalidate) {
      invalidate()
    }
  }

  override fun onGetTemplate(): Template {
    Log.d(TAG, "onGetTemplate for $marker")
    template?.let {
      return it
    }

    if (isCluster) {
      return NavigationTemplate.Builder().apply {
        setActionStrip(ActionStrip.Builder().apply { addAction(Action.APP_ICON) }.build()).build()
      }.build()
    }

    val appName = AppInfo.getApplicationLabel(carContext)

    return MessageTemplate.Builder(appName).apply {
      setIcon(CarIcon.APP_ICON)
    }.build()
  }

  companion object {
    const val TAG = "CarScreen"
  }
}
