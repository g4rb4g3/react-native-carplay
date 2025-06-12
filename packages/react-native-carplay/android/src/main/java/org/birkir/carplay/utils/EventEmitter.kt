package org.birkir.carplay.utils

import android.util.Log
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter

class EventEmitter(
  private var reactContext: ReactContext? = null,
  private var templateId: String? = null
) {

  companion object {
    const val Telemetry = "telemetry"
    const val DidConnect = "didConnect"
    const val DidDisconnect = "didDisconnect"
    const val DidFinish = "didFinish" //CarPlayService finished/killed
    const val SafeAreaInsetsDidChange = "safeAreaInsetsDidChange"
    const val AppearanceDidChange = "appearanceDidChange"

    // interface
    const val BarButtonPressed = "barButtonPressed"
    const val BackButtonPressed = "backButtonPressed"
    const val DidAppear = "didAppear"
    const val DidDisappear = "didDisappear"
    const val WillAppear = "willAppear"
    const val WillDisappear = "willDisappear"
    const val ButtonPressed = "buttonPressed"
    const val PoppedToRoot = "poppedToRoot"
    const val VoiceCommand = "voiceCommand"

    // grid
    const val GridButtonPressed = "gridButtonPressed"

    // information
    const val ActionButtonPressed = "actionButtonPressed"

    // list
    const val DidSelectListItem = "didSelectListItem"

    // search
    const val UpdatedSearchText = "updatedSearchText"
    const val SearchButtonPressed = "searchButtonPressed"
    const val SelectedResult = "selectedResult"

    // tab bar
    const val DidSelectTemplate = "didSelectTemplate"

    // now playing
    const val UpNextButtonPressed = "upNextButtonPressed"
    const val AlbumArtistButtonPressed = "albumArtistButtonPressed"

    // poi
    const val DidSelectPointOfInterest = "didSelectPointOfInterest"

    // map
    const val MapButtonPressed = "mapButtonPressed"
    const val DidUpdatePanGestureWithTranslation = "didUpdatePanGestureWithTranslation"
    const val DidEndPanGestureWithVelocity = "didEndPanGestureWithVelocity"
    const val PanBeganWithDirection = "panBeganWithDirection"
    const val PanEndedWithDirection = "panEndedWithDirection"
    const val PanWithDirection = "panWithDirection"
    const val DidBeginPanGesture = "didBeginPanGesture"
    const val DidDismissPanningInterface = "didDismissPanningInterface"
    const val WillDismissPanningInterface = "willDismissPanningInterface"
    const val DidShowPanningInterface = "didShowPanningInterface"
    const val DidUpdatePinchGesture = "didUpdatePinchGesture"
    const val DidPress = "didPress"
    const val DidDismissNavigationAlert = "didDismissNavigationAlert"
    const val WillDismissNavigationAlert = "willDismissNavigationAlert"
    const val DidShowNavigationAlert = "didShowNavigationAlert"
    const val WillShowNavigationAlert = "willShowNavigationAlert"
    const val DidCancelNavigation = "didCancelNavigation"
    const val DidEnableAutoDrive = "didEnableAutoDrive"
    const val AlertActionPressed = "alertActionPressed"
    const val SelectedPreviewForTrip = "selectedPreviewForTrip"
    const val StartedTrip = "startedTrip"
  }

  fun telemetry(data: WritableMap) {
    emit(Telemetry, data)
  }

  fun didConnect() {
    Log.d("EventEmitter", "Did connect")
    emit(DidConnect)
  }

  fun didDisconnect() {
    emit(DidDisconnect)
  }

  fun didFinish() {
    emit(DidFinish)
  }

  fun buttonPressed(buttonId: String) {
    emit(ButtonPressed, Arguments.createMap().apply {
      putString("buttonId", buttonId)
    })
  }

  fun barButtonPressed(templateId: String, buttonId: String) {
    emit(BarButtonPressed, Arguments.createMap().apply {
      putString("buttonId", buttonId)
    })
  }

  fun backButtonPressed(templateId: String?) {
    emit(BackButtonPressed, Arguments.createMap().apply {
      templateId?.let { putString("templateId", templateId) }
    })
  }

  fun didSelectListItem(id: String, index: Int) {
    emit(DidSelectListItem, Arguments.createMap().apply {
      putString("id", id)
      putInt("index", index)
    })
  }

  fun didSelectTemplate(selectedTemplateId: String) {
    emit(DidSelectTemplate, Arguments.createMap().apply {
      putString("selectedTemplateId", selectedTemplateId)
    })
  }

  fun updatedSearchText(searchText: String) {
    emit(UpdatedSearchText, Arguments.createMap().apply {
      putString("searchText", searchText)
    })
  }

  fun searchButtonPressed(searchText: String) {
    emit(SearchButtonPressed, Arguments.createMap().apply {
      putString("searchText", searchText)
    })
  }

  fun alertActionPressed(id: Int, type: String, reason: String? = null) {
    emit(AlertActionPressed, Arguments.createMap().apply {
      putInt("id", id)
      putString("type", type)
      reason?.let { putString("reason", reason) }
    })
  }

  fun selectedResult(index: Int, id: String?) {
    emit(SelectedResult, Arguments.createMap().apply {
      id?.let { putString("id", id) }
      putInt("index", index)
    })
  }

  fun gridButtonPressed(id: String, index: Int) {
    val event = Arguments.createMap()
    event.putString("id", id)
    event.putInt("index", index)
    emit(GridButtonPressed, event)
  }

  fun didShowPanningInterface() {
    emit(DidShowPanningInterface)
  }

  fun willAppear() {
    emit(WillAppear)
  }

  fun didAppear() {
    emit(DidAppear)
  }

  fun willDisappear() {
    emit(WillDisappear)
  }

  fun didDisappear() {
    emit(DidDisappear)
  }

  fun didDismissPanningInterface() {
    emit(DidDismissPanningInterface)
  }

  fun didUpdatePanGestureWithTranslation(distanceX: Float, distanceY: Float) {
    emit(DidUpdatePanGestureWithTranslation, Arguments.createMap().apply {
      putMap("translation", Arguments.createMap().apply {
        putDouble("x", distanceX.toDouble())
        putDouble("y", distanceY.toDouble())
      })
    })
  }

  fun didUpdatePinchGesture(focusX: Float, focusY: Float, scaleFactor: Float) {
    emit(DidUpdatePinchGesture, Arguments.createMap().apply {
      putDouble("x", focusX.toDouble())
      putDouble("y", focusY.toDouble())
      putDouble("scaleFactor", scaleFactor.toDouble())
    })
  }

  fun didPress(x: Float, y: Float) {
    emit(DidPress, Arguments.createMap().apply {
      putDouble("x", x.toDouble())
      putDouble("y", y.toDouble())
    })
  }

  fun safeAreaInsetsDidChange(top: Int, bottom: Int, left: Int, right: Int) {
    emit(SafeAreaInsetsDidChange, Arguments.createMap().apply {
      putInt("top", top)
      putInt("bottom", bottom)
      putInt("left", left)
      putInt("right", right)
      putString("id", templateId)
    })
  }

  fun didCancelNavigation() {
    emit(DidCancelNavigation)
  }

  fun didEnableAutoDrive() {
    emit(DidEnableAutoDrive)
  }

  fun didPopToRoot() {
    emit(PoppedToRoot)
  }

  fun appearanceDidChange(isDarkMode: Boolean) {
    emit(AppearanceDidChange, Arguments.createMap().apply {
      putString("colorScheme", if (isDarkMode) "dark" else "light")
    })
  }

  fun voiceCommand(data: WritableMap) {
    emit(VoiceCommand, data)
  }

  private fun emit(eventName: String, data: WritableMap = Arguments.createMap()) {
    if (reactContext == null) {
      Log.e("RNCarPlay", "Could not send event $eventName. React context is null!")
      return
    }
    if (templateId != null && !data.hasKey("templateId")) {
      data.putString("templateId", templateId)
    }
    reactContext!!
      .getJSModule(RCTDeviceEventEmitter::class.java)
      .emit(eventName, data)
  }
}
