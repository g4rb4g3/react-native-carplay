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
    private lateinit var navigationManager: NavigationManager
    private lateinit var eventEmitter: EventEmitter
    private lateinit var carContext: CarContext

    private var isNavigating = MutableLiveData(false)
    val isNavigatingLiveData: LiveData<Boolean> get() = isNavigating

    fun init(carContext: CarContext, eventEmitter: EventEmitter) {
        this.eventEmitter = eventEmitter
        this.carContext = carContext

        if (this::navigationManager.isInitialized) {
            return
        }

        navigationManager = carContext.getCarService(NavigationManager::class.java)

        navigationManager.setNavigationManagerCallback(object : NavigationManagerCallback {
            override fun onAutoDriveEnabled() {
                eventEmitter.didEnableAutoDrive()
            }

            override fun onStopNavigation() {
                eventEmitter.didCancelNavigation()
                isNavigating.value = false
            }
        })
    }

    fun isInitialized(): Boolean =
        this::navigationManager.isInitialized && this::eventEmitter.isInitialized

    fun navigationStarted() {
        navigationManager.navigationStarted()
        isNavigating.value = true
    }

    fun navigationEnded() {
        navigationManager.navigationEnded()
        isNavigating.value = false
    }

    fun updateTrip(tripConfig: ReadableMap) {
        if (isNavigating.value != true) {
            return
        }

        val trip = Trip.Builder().apply {
            tripConfig.getArray("steps")?.let { steps ->
                for (i in 0 until(steps.size())) {
                    val stepConfig = steps.getMap(i)
                    stepConfig?.let { stepMap ->
                        val step = Parser.parseStep(stepMap, carContext)
                        stepMap.getMap("stepTravelEstimate")?.let { travelEstimate ->
                            val stepTravelEstimate = Parser.parseTravelEstimate(travelEstimate)
                            addStep(step, stepTravelEstimate)
                        }
                    }
                }
            }
            tripConfig.getArray("destinations")?.let { destinations ->
                for (i in 0 until(destinations.size())) {
                    val destinationConfig = destinations.getMap(i)
                    destinationConfig?.let { destMap ->
                        val destination = Parser.parseDestination(destMap, carContext)
                        destMap.getMap("destinationTravelEstimate")?.let { travelEstimate ->
                            val destinationTravelEstimate = Parser.parseTravelEstimate(travelEstimate)
                            addDestination(destination, destinationTravelEstimate)
                        }
                    }
                }
            }
        }.build()

        navigationManager.updateTrip(trip)
    }
}