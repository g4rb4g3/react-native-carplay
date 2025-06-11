package org.birkir.carplay.utils

import android.app.Presentation
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import com.facebook.react.ReactApplication
import com.facebook.react.ReactRootView
import com.facebook.react.uimanager.DisplayMetricsHolder
import org.birkir.carplay.BuildConfig

/**
 * Renders the view tree into a surface using VirtualDisplay. It runs the ReactNative component registered
 */

class VirtualRenderer(
  private val context: CarContext,
  private val moduleName: String,
  private val isCluster: Boolean
) {

  private var rootView: ReactRootView? = null
  private var emitter: EventEmitter

  /**
   * since react-native renders everything with the density/scaleFactor from the main display we have to adjust scaling on AA to take this into account
   */
  private val mainScreenDensity = DisplayMetricsHolder.getScreenDisplayMetrics().density
  private val virtualScreenDensity = context.resources.displayMetrics.density
  val scale = virtualScreenDensity / mainScreenDensity * BuildConfig.CARPLAY_SCALE_FACTOR

  init {
    val reactContext =
      (context.applicationContext as ReactApplication).reactNativeHost.reactInstanceManager.currentReactContext
    emitter = EventEmitter(reactContext = reactContext, templateId = moduleName)

    context.getCarService(AppManager::class.java).setSurfaceCallback(object : SurfaceCallback {

      var height = 0
      var width = 0

      override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        val surface = surfaceContainer.surface
        if (surface == null) {
          Log.w(TAG, "surface is null")
        } else {
          renderPresentation(surfaceContainer)
        }

        height = surfaceContainer.height
        width = surfaceContainer.width
      }

      override fun onClick(x: Float, y: Float) {
        emitter.didPress(x = x / scale, y = y / scale)
      }

      override fun onScale(focusX: Float, focusY: Float, scaleFactor: Float) {
        emitter.didUpdatePinchGesture(
          focusX = focusX / scale,
          focusY = focusY / scale,
          scaleFactor = scaleFactor
        )
      }

      override fun onScroll(distanceX: Float, distanceY: Float) {
        emitter.didUpdatePanGestureWithTranslation(
          distanceX = -distanceX / scale,
          distanceY = -distanceY / scale
        )
      }

      override fun onVisibleAreaChanged(visibleArea: Rect) {
        if (visibleArea.top == 0 && visibleArea.bottom == 0 && visibleArea.left == 0 && visibleArea.right == 0) {
          // we sometimes get no proper visible area on the cluster display
          return
        }
        val top = (visibleArea.top / BuildConfig.CARPLAY_SCALE_FACTOR).toInt()
        val bottom = ((height - visibleArea.bottom) / BuildConfig.CARPLAY_SCALE_FACTOR).toInt()
        val left = (visibleArea.left / BuildConfig.CARPLAY_SCALE_FACTOR).toInt()
        val right = ((width - visibleArea.right) / BuildConfig.CARPLAY_SCALE_FACTOR).toInt()
        emitter.safeAreaInsetsDidChange(top = top, bottom = bottom, left = left, right = right)
      }
    })
  }

  private fun renderPresentation(container: SurfaceContainer) {
    val name = if (isCluster) "AndroidAutoClusterMapTemplate" else "AndroidAutoMapTemplate"
    val manager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    val display = manager.createVirtualDisplay(
      name,
      container.width,
      container.height,
      container.dpi,
      container.surface,
      DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION,
    )
    val presentation = MapPresentation(context, display.display, moduleName, container)
    presentation.show()
  }

  inner class MapPresentation(
    private val context: CarContext,
    display: Display,
    private val moduleName: String,
    private val surfaceContainer: SurfaceContainer
  ) : Presentation(context, display) {
    override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      val instanceManager =
        (context.applicationContext as ReactApplication).reactNativeHost.reactInstanceManager


      var splashView: ViewGroup? = null

      if (rootView == null) {
        splashView = if (!isCluster) null else getSplashView(context, surfaceContainer.height, surfaceContainer.width)

        Log.d(TAG, "onCreate: rootView is null, initializing rootView")
        val initialProperties = Bundle().apply {
          putString("id", moduleName)
          putString("colorScheme", if (context.isDarkMode) "dark" else "light")
          putBundle("window", Bundle().apply {
            putInt("height", (surfaceContainer.height / BuildConfig.CARPLAY_SCALE_FACTOR).toInt())
            putInt("width", (surfaceContainer.width / BuildConfig.CARPLAY_SCALE_FACTOR).toInt())
            putFloat("scale", context.resources.displayMetrics.density)
          })
        }

        rootView = ReactRootView(context.applicationContext).apply {
          layoutParams = FrameLayout.LayoutParams(
            (surfaceContainer.width / scale).toInt(), (surfaceContainer.height / scale).toInt()
          )
          scaleX = scale
          scaleY = scale
          pivotX = 0f
          pivotY = 0f
          setBackgroundColor(Color.DKGRAY)

          startReactApplication(instanceManager, moduleName, initialProperties)
          runApplication()

          splashView?.let {
            var splashWillDisappear = false

            // register a layout listener to remove the splash screen when the react component is mounted
            viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
              override fun onGlobalLayout() {
                addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                  if (!splashWillDisappear) {
                    splashWillDisappear = true
                    it.animate()
                      .alpha(0f)
                      .setStartDelay(BuildConfig.CARPLAY_CLUSTER_SPLASH_DELAY_MS)
                      .setDuration(BuildConfig.CARPLAY_CLUSTER_SPLASH_DURATION_MS)
                      .withEndAction {
                        (it.parent as ViewGroup).removeView(it)
                        splashView = null
                      }
                  }

                  // Remove this listener to avoid repeated calls
                  viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
              }
            })
          }
        }
      } else {
        (rootView?.parent as? ViewGroup)?.removeView(rootView)
      }
      rootView?.let {
        val rootContainer = FrameLayout(context).apply {
          layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
          )
          clipChildren = false // Allow content to extend beyond bounds
        }

        // add the react root view
        rootContainer.addView(it)

        splashView?.let {
          // and the splash screen above the react root view
          rootContainer.addView(it, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }

        setContentView(rootContainer)
      }
    }
  }

  private fun getSplashView(context: CarContext, containerHeight: Int, containerWidth: Int): LinearLayout {
    val applicationIcon = AppInfo.getApplicationIcon(context)
    val appName = AppInfo.getApplicationLabel(context)

    val maxIconSize = (0.25 * maxOf(containerHeight, containerWidth)).toInt()

    return LinearLayout(context).apply {
      orientation = LinearLayout.VERTICAL
      gravity = Gravity.CENTER
      layoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT
      )
      setBackgroundColor(Color.DKGRAY)

      val iconView = ImageView(context).apply {
        setImageDrawable(applicationIcon)
        layoutParams = LinearLayout.LayoutParams(
          maxIconSize,
          maxIconSize
        ).also {
          it.bottomMargin = 16
        }
      }

      val appNameView = TextView(context).apply {
        text = appName
        setTextColor(Color.WHITE)
        textSize = 20f
        layoutParams = LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.WRAP_CONTENT,
          LinearLayout.LayoutParams.WRAP_CONTENT
        )
      }

      addView(iconView)
      addView(appNameView)
    }
  }

  companion object {
    const val TAG = "VirtualRenderer"
  }
}
