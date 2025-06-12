package org.birkir.carplay.utils

import com.facebook.react.ReactInstanceManager
import com.facebook.react.bridge.ReactContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object ReactContextResolver {
  suspend fun getReactContext(reactInstanceManager: ReactInstanceManager): ReactContext {
    return suspendCancellableCoroutine { continuation ->
      reactInstanceManager.currentReactContext?.let {
        continuation.resume(it)
        return@suspendCancellableCoroutine
      }

      val listener = object : ReactInstanceManager.ReactInstanceEventListener {
        override fun onReactContextInitialized(context: ReactContext) {
          reactInstanceManager.removeReactInstanceEventListener(this)
          continuation.resume(context)
        }
      }
      reactInstanceManager.addReactInstanceEventListener(listener)

      continuation.invokeOnCancellation {
        reactInstanceManager.removeReactInstanceEventListener(listener)
      }

      reactInstanceManager.createReactContextInBackground()
    }
  }
}