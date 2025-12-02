package org.birkir.carplay.utils

import androidx.car.app.CarContext
import androidx.car.app.navigation.NavigationManager
import androidx.car.app.navigation.NavigationManagerCallback
import androidx.car.app.navigation.model.Trip
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.facebook.react.bridge.ReadableMap
import org.birkir.carplay.parser.Parser

object CarNavigationManager {
    private var navigationManager: NavigationManager? = null
    private var eventEmitter: EventEmitter? = null
    private var carContext: CarContext? = null

    private var isNavigating = MutableLiveData(false)
    val isNavigatingLiveData: LiveData<Boolean> get() = isNavigating

    fun init(carContext: CarContext, eventEmitter: EventEmitter) {
        this.eventEmitter = eventEmitter
        this.carContext = carContext

        this.navigationManager = carContext.getCarService(NavigationManager::class.java)
        this.navigationManager?.setNavigationManagerCallback(object : NavigationManagerCallback {
            override fun onAutoDriveEnabled() {
                eventEmitter.didEnableAutoDrive()
            }

            override fun onStopNavigation() {
                eventEmitter.didCancelNavigation()
                isNavigating.value = false
            }
        })
    }

    fun destroy() {
        this.carContext = null
        this.navigationManager = null
        this.eventEmitter = null
        isNavigating.value = false
    }

    fun isInitialized(): Boolean =
        this.navigationManager != null && this.eventEmitter != null

    fun navigationStarted() {
        navigationManager?.let {
            it.navigationStarted()
            isNavigating.value = true
        }
    }

    fun navigationEnded() {
        navigationManager?.let {
            it.navigationEnded()
            isNavigating.value = false
        }
    }

    fun updateTrip(tripConfig: ReadableMap) {
        if (isNavigating.value != true) {
            return
        }

        carContext?.let {
            val trip = Trip.Builder().apply {
                tripConfig.getArray("steps")?.let { steps ->
                    for (i in 0 until (steps.size())) {
                        val stepConfig = steps.getMap(i)
                        stepConfig?.let { stepMap ->
                            val step = Parser.parseStep(stepMap, it)
                            stepMap.getMap("stepTravelEstimate")?.let { travelEstimate ->
                                val stepTravelEstimate = Parser.parseTravelEstimate(travelEstimate)
                                addStep(step, stepTravelEstimate)
                            }
                        }
                    }
                }
                tripConfig.getArray("destinations")?.let { destinations ->
                    for (i in 0 until (destinations.size())) {
                        val destinationConfig = destinations.getMap(i)
                        destinationConfig?.let { destMap ->
                            val destination = Parser.parseDestination(destMap, it)
                            destMap.getMap("destinationTravelEstimate")?.let { travelEstimate ->
                                val destinationTravelEstimate =
                                    Parser.parseTravelEstimate(travelEstimate)
                                addDestination(destination, destinationTravelEstimate)
                            }
                        }
                    }
                }
            }.build()

            navigationManager?.updateTrip(trip)
        }
    }
}