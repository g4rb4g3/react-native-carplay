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
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
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
  private val isCluster: Boolean,
  private val sessionLifecycle: Lifecycle
) {

  private var rootView: ReactRootView? = null
  private var emitter: EventEmitter

  /**
   * since react-native renders everything with the density/scaleFactor from the main display we have to adjust scaling on AA to take this into account
   */
  private val mainScreenDensity = DisplayMetricsHolder.getScreenDisplayMetrics().density
  private val virtualScreenDensity = context.resources.displayMetrics.density
  val reactNativeScale = virtualScreenDensity / mainScreenDensity * BuildConfig.CARPLAY_SCALE_FACTOR

  init {
    val reactContext =
      (context.applicationContext as ReactApplication).reactNativeHost.reactInstanceManager.currentReactContext
    emitter = EventEmitter(reactContext = reactContext, templateId = moduleName)

    val areaDebouncer = Debouncer(200)

    sessionLifecycle.addObserver(object : DefaultLifecycleObserver {
      override fun onDestroy(owner: LifecycleOwner) {
        (rootView?.parent as? ViewGroup)?.removeView(rootView)
        rootView?.unmountReactApplication()
        rootView = null
        sessionLifecycle.removeObserver(this)
      }
    })

    context.getCarService(AppManager::class.java).setSurfaceCallback(object : SurfaceCallback {
      var height = 0
      var width = 0
      var minMargin = Int.MAX_VALUE
      // 12dp seems to be the default margin on AA for the ETA widget and the maneuver so use it as fallback
      val defaultMargin = (12.0 * context.resources.displayMetrics.density).toInt()

      var stableArea = Rect(0, 0, 0, 0)
      var visibleArea = Rect(0, 0, 0, 0)
      val scale = BuildConfig.CARPLAY_SCALE_FACTOR * context.resources.displayMetrics.density

      override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        val surface = surfaceContainer.surface
        if (surface == null) {
          Log.w(TAG, "surface is null")
        } else {
          renderPresentation(surfaceContainer, scale)
        }

        height = surfaceContainer.height
        width = surfaceContainer.width
      }

      override fun onClick(x: Float, y: Float) {
        emitter.didPress(x = x / BuildConfig.CARPLAY_SCALE_FACTOR, y = y / BuildConfig.CARPLAY_SCALE_FACTOR)
      }

      override fun onScale(focusX: Float, focusY: Float, scaleFactor: Float) {
        emitter.didUpdatePinchGesture(
          focusX = focusX / BuildConfig.CARPLAY_SCALE_FACTOR,
          focusY = focusY / BuildConfig.CARPLAY_SCALE_FACTOR,
          scaleFactor = scaleFactor
        )
      }

      override fun onScroll(distanceX: Float, distanceY: Float) {
        emitter.didUpdatePanGestureWithTranslation(
          distanceX = -distanceX / BuildConfig.CARPLAY_SCALE_FACTOR,
          distanceY = -distanceY / BuildConfig.CARPLAY_SCALE_FACTOR
        )
      }

      override fun onVisibleAreaChanged(visibleArea: Rect) {
        this.visibleArea = visibleArea
        areaDebouncer.submit {
          this.minMargin = minMargin.coerceAtMost(minOf(visibleArea.top, visibleArea.left, visibleArea.bottom, visibleArea.right))
          updateSafeAreaInsets()
        }
      }

      override fun onStableAreaChanged(stableArea: Rect) {
        this.stableArea = stableArea
        areaDebouncer.submit {
          this.minMargin = minMargin.coerceAtMost(minOf(stableArea.top, stableArea.left, stableArea.bottom, stableArea.right))
          updateSafeAreaInsets()
        }
      }

      fun updateSafeAreaInsets() {
        if (maxOf(stableArea.top, stableArea.left, stableArea.bottom, stableArea.right) == 0) {
          // wait for stable area to be initialized first
          return
        }

        if (maxOf(visibleArea.top, visibleArea.left, visibleArea.bottom, visibleArea.right) == 0) {
          // wait for visible area to be initialized first
          return
        }

        if (minMargin == 0) {
          // probably legacy AA layout
          val additionalMarginLeft = if (stableArea.left == visibleArea.left) defaultMargin else 0
          val additionalMarginRight = if (stableArea.right == visibleArea.right && visibleArea.right != width) 0 else defaultMargin
          val additionalMarginTop = if (visibleArea.top != stableArea.top || (visibleArea.top > 0 && stableArea.top > 0 && visibleArea.right < width)) 0 else defaultMargin
          val additionalMarginBottom = if (stableArea.bottom == visibleArea.bottom) defaultMargin else 0

          val top = ((visibleArea.top + additionalMarginTop) / scale).toInt()
          val bottom = ((height - visibleArea.bottom + additionalMarginBottom) / scale).toInt()
          val left = ((visibleArea.left + additionalMarginLeft) / scale).toInt()
          val right = ((width - visibleArea.right + additionalMarginRight) / scale).toInt()
          emitter.safeAreaInsetsDidChange(top = top, bottom = bottom, left = left, right = right, isLegacyLayout = true)
        } else {
          // material expression 3 seems to apply always some margin and never reports 0

          val additionalMarginLeft = if (stableArea.left == visibleArea.left) defaultMargin else 0
          val additionalMarginRight = if (stableArea.right == visibleArea.right) defaultMargin else 0

          val top = (visibleArea.top.coerceAtLeast(defaultMargin) / scale).toInt()
          val bottom = ((height - visibleArea.bottom).coerceAtLeast(defaultMargin) / scale).toInt()
          val left = ((visibleArea.left + additionalMarginLeft).coerceAtLeast(defaultMargin) / scale).toInt()
          val right = ((width - visibleArea.right + additionalMarginRight).coerceAtLeast(defaultMargin) / scale).toInt()
          emitter.safeAreaInsetsDidChange(top = top, bottom = bottom, left = left, right = right, isLegacyLayout = false)
        }
      }
    })
  }

  private fun renderPresentation(container: SurfaceContainer, scale: Float) {
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
    val presentation = MapPresentation(context, display.display, moduleName, container, scale)
    presentation.show()
  }

  inner class MapPresentation(
    private val context: CarContext,
    display: Display,
    private val moduleName: String,
    private val surfaceContainer: SurfaceContainer,
    private val scale: Float
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
            putInt("height", (surfaceContainer.height / scale).toInt())
            putInt("width", (surfaceContainer.width / scale).toInt())
            putFloat("scale", scale)
          })
        }

        rootView = ReactRootView(context.applicationContext).apply {
          layoutParams = FrameLayout.LayoutParams(
            (surfaceContainer.width / reactNativeScale).toInt(), (surfaceContainer.height / reactNativeScale).toInt()
          )
          scaleX = reactNativeScale
          scaleY = reactNativeScale
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
