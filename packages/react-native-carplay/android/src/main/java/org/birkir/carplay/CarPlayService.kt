package org.birkir.carplay

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.IBinder
import android.util.Log
import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.SessionInfo
import androidx.car.app.notification.CarAppExtender
import androidx.car.app.validation.HostValidator
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.facebook.react.ReactApplication
import com.facebook.react.ReactInstanceManager
import com.facebook.react.bridge.LifecycleEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.birkir.carplay.parser.Parser
import org.birkir.carplay.utils.CarNavigationManager
import org.birkir.carplay.utils.EventEmitter
import org.birkir.carplay.utils.ReactContextResolver

class CarPlayService : CarAppService(), LifecycleEventListener {
  private lateinit var reactInstanceManager: ReactInstanceManager
  private lateinit var emitter: EventEmitter
  private lateinit var notificationManager: NotificationManager
  private var mServiceBound = false
  private var isSessionStarted = false
  private var isReactAppStarted = false

  private val connection: ServiceConnection = object : ServiceConnection {
    override fun onServiceConnected(
      className: ComponentName, service: IBinder
    ) {
      mServiceBound = true
    }

    override fun onServiceDisconnected(arg0: ComponentName) {
      mServiceBound = false
    }
  }

  private val isNavigatingObserver = Observer<Boolean> {
    if (!it) {
      stopForeground(STOP_FOREGROUND_REMOVE)
      return@Observer
    }

    if (!isSessionStarted && !isReactAppStarted) {
      Log.w(TAG, "CarSession and ReactApp not running, unable to start foreground service!");
      return@Observer
    }

    val isLocationPermissionGranted =
      checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED || checkSelfPermission(
        Manifest.permission.ACCESS_FINE_LOCATION
      ) == PackageManager.PERMISSION_GRANTED

    if (!isLocationPermissionGranted) {
      Log.w(TAG, "location permission not granted, unable to start foreground service!")
      return@Observer
    }

    try {
      startForeground(
        NOTIFICATION_ID,
        createNotification(null, null, null)
      )
    } catch (e: SecurityException) {
      Log.e(TAG, "failed to start foreground service", e)
    }
  }

  private val notificationObserver = Observer<CarNotification?> {
    it?.let {
      val icon = it.largeIcon?.let { icon ->
        Parser.parseBitmap(icon, context = this@CarPlayService)
      }
      val notification = createNotification(it.title, it.text, icon)
      notificationManager.notify(NOTIFICATION_ID, notification)
    }
  }

  override fun onCreate() {
    Log.d(TAG, "CarPlayService onCreate")
    super.onCreate()
    reactInstanceManager = (application as ReactApplication).reactNativeHost.reactInstanceManager

    notificationManager = this.getSystemService(NotificationManager::class.java)

    val appLabel = this.packageManager.getApplicationLabel(this.applicationInfo)
    getSystemService(NotificationManager::class.java).createNotificationChannel(
      NotificationChannel(
        CHANNEL_ID, appLabel, NotificationManager.IMPORTANCE_DEFAULT
      )
    )

    CarNavigationManager.isNavigatingLiveData.observeForever(isNavigatingObserver)
    CarPlayModule.notifier.observeForever(notificationObserver)

    CoroutineScope(Dispatchers.Main).launch {
      val reactContext = ReactContextResolver.getReactContext(reactInstanceManager)
      emitter = EventEmitter(reactContext)
      reactContext.addLifecycleEventListener(this@CarPlayService)
    }
  }

  @SuppressLint("PrivateResource")
  override fun createHostValidator(): HostValidator {
    return if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
      HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    } else {
      HostValidator.Builder(applicationContext)
        .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample).build()
    }
  }

  override fun onCreateSession(sessionInfo: SessionInfo): Session {
    Log.d(
      TAG,
      "onCreateSession: sessionId = ${sessionInfo.sessionId}, display = ${sessionInfo.displayType}"
    )

    val session = CarPlaySession(reactInstanceManager, sessionInfo)

    if (sessionInfo.displayType == SessionInfo.DISPLAY_TYPE_CLUSTER) {
      return session
    }

    session.lifecycle.addObserver(object : DefaultLifecycleObserver {
      override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)

        // let the headlessTask know that AA is ready and make sure Timers are working even when the screen is off
        val serviceIntent = Intent(applicationContext, CarPlayHeadlessTaskService::class.java)
        bindService(serviceIntent, connection, BIND_AUTO_CREATE)
      }

      override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)

        if (mServiceBound) {
          unbindService(connection)
          mServiceBound = false
        }

        this@CarPlayService.stopForeground(STOP_FOREGROUND_REMOVE)
      }

      override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)

        isSessionStarted = true
      }

      override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)

        isSessionStarted = false
      }
    })

    return session
  }

  override fun onDestroy() {
    stopForeground(STOP_FOREGROUND_REMOVE)
    CarNavigationManager.isNavigatingLiveData.removeObserver(isNavigatingObserver)
    CarPlayModule.notifier.removeObserver(notificationObserver)
    notificationManager.cancelAll()
    super.onDestroy()
  }

  private fun createNotification(title: String?, text: String?, largeIcon: Bitmap?): Notification {
    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_notification)
      .setOngoing(true)
      .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
      .setOnlyAlertOnce(true)
      .setWhen(System.currentTimeMillis())
      .setPriority(NotificationManager.IMPORTANCE_LOW)
      .extend(
        CarAppExtender.Builder().setImportance(NotificationManagerCompat.IMPORTANCE_LOW).build()
      )
      .apply {
        title?.let {
          setContentTitle(it)
        }
        text?.let {
          setContentText(it)
          setTicker(it)
        }
        largeIcon?.let {
          setLargeIcon(it)
        }
      }
      .build()
  }

  companion object {
    var TAG = "CarPlayService"
    private const val NOTIFICATION_ID = 2
    private const val CHANNEL_ID = "CarPlayServiceChannel"
  }

  override fun onHostDestroy() {
    stopSelf()
  }

  override fun onHostPause() {
    isReactAppStarted = false
  }

  override fun onHostResume() {
    isReactAppStarted = true
  }
}
