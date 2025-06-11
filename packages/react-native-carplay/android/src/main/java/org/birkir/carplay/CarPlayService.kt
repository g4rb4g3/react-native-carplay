package org.birkir.carplay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
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
import org.birkir.carplay.utils.CarNavigationManager
import org.birkir.carplay.utils.EventEmitter

class CarPlayService : CarAppService() {
  private lateinit var reactInstanceManager: ReactInstanceManager
  private lateinit var emitter: EventEmitter
  private lateinit var notificationManager: NotificationManager
  private var mServiceBound = false

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
    Log.d(TAG, "isNavigating $it")
    if (!it) {
      stopForeground(STOP_FOREGROUND_REMOVE)
      return@Observer
    }

    startForeground(
      NOTIFICATION_ID,
      createNotification(null, null, null)
    )
  }

  private val notificationObserver = Observer<CarNotification?> {
    it?.let {
      val notification = createNotification(it.title, it.text, it.largeIcon)
      notificationManager.notify(NOTIFICATION_ID, notification)
    }
  }

  override fun onCreate() {
    Log.d(TAG, "CarPlayService onCreate")
    super.onCreate()
    reactInstanceManager = (application as ReactApplication).reactNativeHost.reactInstanceManager

    emitter = EventEmitter(reactContext = reactInstanceManager.currentReactContext)
    notificationManager = this.getSystemService(NotificationManager::class.java)

    val appLabel = this.packageManager.getApplicationLabel(this.applicationInfo)
    getSystemService(NotificationManager::class.java).createNotificationChannel(
      NotificationChannel(
        CHANNEL_ID, appLabel, NotificationManager.IMPORTANCE_DEFAULT
      )
    )

    CarNavigationManager.isNavigatingLiveData.observeForever(isNavigatingObserver)
    CarPlayModule.notifier.observeForever(notificationObserver)
  }

  override fun createHostValidator(): HostValidator {
    return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
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
      }
    })

    return session
  }

  override fun onDestroy() {
    super.onDestroy()
    CarNavigationManager.isNavigatingLiveData.removeObserver(isNavigatingObserver)
    CarPlayModule.notifier.removeObserver(notificationObserver)
    emitter.didFinish()
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
}
