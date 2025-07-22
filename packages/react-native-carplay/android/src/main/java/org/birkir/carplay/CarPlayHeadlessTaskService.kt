package org.birkir.carplay

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.facebook.react.HeadlessJsTaskService
import com.facebook.react.bridge.Arguments
import com.facebook.react.jstasks.HeadlessJsTaskConfig

class CarPlayHeadlessTaskService : HeadlessJsTaskService() {
    // we use a bound service so it will never be killed when Android Auto is active
    inner class LocalBinder: Binder() {
        fun getService() = this@CarPlayHeadlessTaskService
    }

    private val mBinder = LocalBinder()

    override fun getTaskConfig(intent: Intent?) = HeadlessJsTaskConfig(
        // we allow this task to run in foreground since it just makes sure timers are still working when the screen is off
        "CarPlayHeadlessJsTask", Arguments.createMap(), 0, true
    )

    override fun onBind(intent: Intent?): IBinder {
        super.startTask(getTaskConfig(intent))
        return mBinder
    }
}