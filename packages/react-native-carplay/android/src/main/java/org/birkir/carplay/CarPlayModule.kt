package org.birkir.carplay

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.ScreenManager
import androidx.car.app.model.Alert
import androidx.car.app.model.AlertCallback
import androidx.car.app.model.TabTemplate
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Callback
import com.facebook.react.bridge.LifecycleEventListener
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableNativeMap
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.modules.debug.DevSettingsModule
import org.birkir.carplay.parser.Parser
import org.birkir.carplay.parser.RCTPermissionRequestTemplate
import org.birkir.carplay.parser.TemplateParser
import org.birkir.carplay.screens.CarScreen
import org.birkir.carplay.screens.CarScreenContext
import org.birkir.carplay.telemetry.CarPlayTelemetryObserver
import org.birkir.carplay.utils.CarNavigationManager
import org.birkir.carplay.utils.EventEmitter
import org.birkir.carplay.utils.PlayService
import java.lang.UnsupportedOperationException
import java.util.WeakHashMap

data class CarNotification (val title: String?, val text: String?, val largeIcon: ReadableMap?)

@ReactModule(name = CarPlayModule.NAME)
class CarPlayModule internal constructor(private val reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {
  private lateinit var carContext: CarContext
  private lateinit var sessionLifecycle: Lifecycle
  private var screenManager: ScreenManager? = null
  private val carScreens: WeakHashMap<String, CarScreen> = WeakHashMap()
  private val carScreenContexts: WeakHashMap<CarScreen, CarScreenContext> =
    WeakHashMap()
  val clusterScreens: WeakHashMap<CarScreen, CarScreenContext> = WeakHashMap()
  private val handler: Handler = Handler(Looper.getMainLooper())

  // Global event emitter (no templateId's)
  private var eventEmitter = EventEmitter(reactContext)

  init {
    reactContext.addLifecycleEventListener(object : LifecycleEventListener {
      override fun onHostResume() {
        reactContext.getNativeModule(DevSettingsModule::class.java)
          ?.addMenuItem("Reload Android Auto")
      }

      override fun onHostPause() {}
      override fun onHostDestroy() {}
    })
  }

  override fun getName(): String {
    return NAME
  }

  fun setCarContext(carContext: CarContext, currentCarScreen: CarScreen, sessionLifecycle: Lifecycle) {
    this.carContext = carContext
    this.sessionLifecycle = sessionLifecycle
    screenManager = currentCarScreen.screenManager
    carScreens["root"] = currentCarScreen
    carContext.onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        val topScreen = screenManager?.top
        if (topScreen is CarScreen) {
          // handle back key only for rn-cp screens
          eventEmitter.backButtonPressed(screenManager?.top?.marker)
          return
        }

        isEnabled = false
        carContext.onBackPressedDispatcher.onBackPressed()
        isEnabled = true

      }
    })
    eventEmitter.didConnect()
  }

  private fun parseTemplate(
    config: ReadableMap,
    carScreenContext: CarScreenContext
  ): Template {
    val factory = TemplateParser(carContext, carScreenContext)
    return factory.parse(config)
  }

  @ReactMethod
  fun checkForConnection() {
    eventEmitter.didConnect()
  }

  @ReactMethod
  fun createTemplate(templateId: String, config: ReadableMap, callback: Callback?) {
    handler.post {
      Log.d(TAG, "Creating template $templateId")

      try {
        createScreen(templateId, config)
        callback?.invoke()
      } catch (err: IllegalArgumentException) {
        val args = Arguments.createMap()
        args.putString("error", "Failed to parse template '$templateId': ${err.message}")
        callback?.invoke(args)
      }
    }
  }

  @ReactMethod
  fun updateTemplate(templateId: String, config: ReadableMap) {
    handler.post {
      carScreens[templateId]?.let { screen ->
        carScreenContexts[screen]?.let { carScreenContext ->
          val template = parseTemplate(config, carScreenContext)
          val isSurfaceTemplate = config.hasKey("render") && config.getBoolean("render")
          screen.setTemplate(template = template, invalidate = true, isSurfaceTemplate = isSurfaceTemplate, sessionLifecycle = sessionLifecycle)

          if (template is NavigationTemplate) {
            config.getMap("trip")?.let {
              CarNavigationManager.updateTrip(it)
            }
            clusterScreens.filter { it.key.template is NavigationTemplate }.forEach{
              val clusterTemplate = parseTemplate(config, it.value)
              // cluster can hold NavigationTemplate only and always has a surface to render to
              it.key.setTemplate(template = clusterTemplate, invalidate = true, isSurfaceTemplate = true, sessionLifecycle = sessionLifecycle)
            }
          }
          if (template is TabTemplate) {
            carScreens[carScreenContext.screenMarker]?.setTemplate(template, invalidate = true, isSurfaceTemplate = true, sessionLifecycle = sessionLifecycle)
            carScreens[carScreenContext.screenMarker]?.invalidate()
          }
        }
      }
    }
  }

  @ReactMethod
  fun setRootTemplate(templateId: String, animated: Boolean?) {
    Log.d(TAG, "set Root Template for $templateId")
    handler.post {
      screenManager?.let {
        val screen = getScreen(templateId)
        if (screen != null) {
          it.popToRoot()
          val root = try {
            it.top
          } catch (_: NullPointerException) {
            null
          }
          it.push(screen)

          if (screen.template is NavigationTemplate) {
            CarNavigationManager.init(
              carContext = carContext,
              eventEmitter = EventEmitter(reactContext = reactContext, templateId = templateId)
            )
          }

          screen.lifecycle.addObserver(object: DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
              eventEmitter.didPopToRoot()
            }

            override fun onDestroy(owner: LifecycleOwner) {
              screen.lifecycle.removeObserver(this)
            }
          })

          if (root != null) {
            if (root is CarScreen) {
              removeScreen(root)
            }
            it.remove(root)
          }
        }
      }
    }
  }

  @ReactMethod
  fun pushTemplate(templateId: String, animated: Boolean?) {
    handler.post {
      val screen = getScreen(templateId)
      if (screen != null) {
        screenManager?.push(screen)
      }
    }
  }

  @ReactMethod
  fun popToTemplate(templateId: String, animated: Boolean?) {
    handler.post {
      screenManager?.popTo(templateId)
    }
  }

  @ReactMethod
  fun popTemplate(animated: Boolean?) {
    val screenManager = screenManager ?: return

    if (screenManager.stackSize == 0) {
      return
    }

    handler.post {
      screenManager.pop()
      screenManager.top.invalidate()
    }
  }

  @ReactMethod
  fun presentTemplate(templateId: String?, animated: Boolean?) {
    // void
  }

  @ReactMethod
  fun dismissTemplate(templateId: String?, animated: Boolean?) {
    // void
  }

  // pragma: Android Auto only stuff

  @ReactMethod
  fun toast(text: String, isLongDurationToast: Boolean) {
    val duration = if (isLongDurationToast) CarToast.LENGTH_LONG else CarToast.LENGTH_SHORT
    CarToast.makeText(carContext, text, duration).show()
  }

  @ReactMethod
  fun alert(props: ReadableMap) {
    handler.post {
      val id = props.getInt("id")
      val title = Parser.parseCarText(props.getString("title")!!, props)
      val duration = props.getInt("duration").toLong()
      val alert = Alert.Builder(id, title, duration).apply {
        setCallback(object : AlertCallback {
          override fun onCancel(reason: Int) {
            val reasonString = when (reason) {
              AlertCallback.REASON_TIMEOUT -> "timeout"
              AlertCallback.REASON_USER_ACTION -> "user"
              AlertCallback.REASON_NOT_SUPPORTED -> "notSupported"
              else -> "unknown"
            }
            // by posting this later we make sure the button press is fired before the dismissal
            handler.postDelayed({
              eventEmitter.didDismissNavigationAlert(
                id,
                "cancel",
                reasonString
              )
            }, 500)
          }

          override fun onDismiss() {
            // by posting this later we make sure the button press is fired before the dismissal
            handler.postDelayed({ eventEmitter.didDismissNavigationAlert(id, "dismiss") }, 500)
          }
        })
        props.getString("subtitle")?.let { setSubtitle(Parser.parseCarText(it, props)) }
        props.getMap("image")?.let { setIcon(Parser.parseCarIcon(it, carContext)) }
        props.getArray("actions")?.let {
          for (i in 0 until it.size()) {
            addAction(Parser.parseAction(map = it.getMap(i), context = carContext, eventEmitter = eventEmitter))
          }
        }
      }.build()
      eventEmitter.willShowNavigationAlert(id)
      carContext.getCarService(AppManager::class.java).showAlert(alert)
    }
  }

  @ReactMethod
  fun dismissAlert(alertId: Int) {
    carContext.getCarService(AppManager::class.java).dismissAlert(alertId)
  }

  @ReactMethod
  fun invalidate(templateId: String) {
    val screenManager = screenManager ?: return
    val screen = getScreen(templateId) ?: return

    handler.post {
      if (screen === screenManager.top) {
        Log.d(TAG, "Invalidated screen $templateId")
        screen.invalidate()
      }
    }
  }

  @ReactMethod
  fun reload() {
    val intent = Intent("org.birkir.carplay.APP_RELOAD")
    reactContext.sendBroadcast(intent)
  }

  @ReactMethod
  fun getHostInfo(promise: Promise) {
    return promise.resolve(Arguments.createMap().apply {
      carContext.hostInfo?.packageName?.let { putString("packageName", it) }
      carContext.hostInfo?.uid?.let { putInt("uid", it) }
    })
  }

  // Others

  @ReactMethod
  fun addListener(eventName: String) {
    Log.d(TAG, "listener added $eventName")
  }

  @ReactMethod
  fun removeListeners(count: Int) {
    Log.d(TAG, "remove listeners $count")
  }

  @ReactMethod
  fun checkForDashboardConnection() {
    //TODO
  }

  @ReactMethod
  fun createDashboard(dashboardId: String, config: ReadableMap) {
    //TODO
  }

  @ReactMethod
  fun updateDashboardShortcutButtons(config: ReadableMap) {
    //TODO
  }

  @ReactMethod
  fun updateListTemplateSections(templateId: String, config: ReadableArray) {
    //TODO
  }

  @ReactMethod
  fun updateListTemplateItem(templateId: String, config: ReadableMap) {
    //TODO
  }

  @ReactMethod
  fun getMaximumListItemCount(templateId: String) {
    //TODO
  }

  @ReactMethod
  fun getMaximumListItemImageSize(templateId: String) {
    //TODO
  }

  @ReactMethod
  fun getMaximumNumberOfGridImages(templateId: String) {
    //TODO
  }

  @ReactMethod
  fun getMaximumListImageRowItemImageSize(templateId: String) {
    //TODO
  }

  @ReactMethod
  fun popToRootTemplate(animated: Boolean?) {
    handler.post {
      screenManager?.popToRoot()
    }
  }

  @ReactMethod
  fun navigationStarted(promise: Promise) {
    if (!CarNavigationManager.isInitialized()) {
      promise.reject(UnsupportedOperationException("CarNavigationManager not initialized, make sure to set a navigation root template first!"))
    }
    handler.post {
      CarNavigationManager.navigationStarted()
    }
    promise.resolve(null)
  }

  @ReactMethod
  fun navigationEnded(promise: Promise) {
    if (!CarNavigationManager.isInitialized()) {
      promise.reject(UnsupportedOperationException("CarNavigationManager not initialized, make sure to set a navigation root template first!"))
      return
    }
    handler.post {
      CarNavigationManager.navigationEnded()
    }
    promise.resolve(null)
  }

  @ReactMethod
  fun startTelemetryObserver(promise: Promise) {
    CarPlayTelemetryObserver.startTelemetryObserver(carContext, eventEmitter, promise)
  }

  @ReactMethod
  fun stopTelemetryObserver() {
    CarPlayTelemetryObserver.stopTelemetryObserver()
  }

  @ReactMethod
  fun notify(title: String, text: String, largeIcon: ReadableMap?) {
    notification.postValue(CarNotification(title, text, largeIcon))
  }

  @ReactMethod
  fun requestPermissions(permissions: ReadableArray, message: String, primaryAction: ReadableMap, headerAction: ReadableMap, promise: Promise) {
    val list = mutableListOf<String>()
    for (i in 0 until permissions.size()) {
      permissions.getString(i)?.let { list.add(it) }
    }
    handler.post {
      screenManager?.push(RCTPermissionRequestTemplate(carContext, list, message, primaryAction, headerAction, promise))
    }
  }

  @ReactMethod
  fun getPlayServicesAvailable(promise: Promise) {
    promise.resolve(PlayService.isPlayServiceAvailable(carContext))
  }

  @ReactMethod
  fun getTopTemplate(promise: Promise) {
    handler.post {
      promise.resolve(screenManager?.screenStack?.firstOrNull()?.marker)
    }
  }

  @ReactMethod
  fun getRootTemplate(promise: Promise) {
    handler.post {
      promise.resolve(screenManager?.screenStack?.lastOrNull()?.marker)
    }
  }

  private fun createCarScreenContext(screen: CarScreen, emitter: EventEmitter): CarScreenContext {
    val templateId = screen.marker!!
    return CarScreenContext(templateId, emitter, carScreens)
  }

  private fun createScreen(templateId: String, templateConfig: ReadableMap?): CarScreen? {
    if (templateConfig != null) {
      val emitter = EventEmitter(reactContext, templateId)
      val screen = CarScreen(carContext, emitter)
      screen.marker = templateId

      // context
      carScreenContexts.remove(screen)
      val carScreenContext = createCarScreenContext(screen, emitter)
      carScreenContexts[screen] = carScreenContext

      val template = parseTemplate(templateConfig, carScreenContext)
      val isSurfaceTemplate = templateConfig.hasKey("render") && templateConfig.getBoolean("render")
      screen.setTemplate(template = template, isSurfaceTemplate = isSurfaceTemplate, sessionLifecycle = sessionLifecycle)
      carScreens[templateId] = screen

      return screen
    }
    return null
  }

  private fun getScreen(name: String): CarScreen? {
    return carScreens[name] ?: createScreen(name, null)
  }

  private fun removeScreen(screen: CarScreen?) {
    val params = WritableNativeMap()
    params.putString("screen", screen!!.marker)
    carScreens.values.remove(screen)
  }

  companion object {
    const val NAME = "RNCarPlay"
    const val TAG = "CarPlay"
    const val APP_RELOAD = "org.birkir.carplay.AppReload"

    private val notification = MutableLiveData<CarNotification?>(null)
    val notifier: LiveData<CarNotification?> get() = notification
  }
}
