package org.birkir.carplay

import android.content.Intent
import com.facebook.react.HeadlessJsTaskService
import com.facebook.react.bridge.Arguments
import com.facebook.react.jstasks.HeadlessJsTaskConfig

class CarPlayHeadlessTaskService : HeadlessJsTaskService() {

    override fun getTaskConfig(intent: Intent?) = HeadlessJsTaskConfig(
        // we allow this task to run in foreground since it just makes sure timers are still working when the screen is off
        "CarPlayHeadlessJsTask", Arguments.createMap(), 0, true
    )
}