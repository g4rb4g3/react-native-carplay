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
                    val step = Parser.parseStep(stepConfig, carContext)
                    val stepTravelEstimate =  Parser.parseTravelEstimate(stepConfig.getMap("stepTravelEstimate")!!)
                    addStep(step, stepTravelEstimate)
                }
            }
            tripConfig.getArray("destinations")?.let { destinations ->
                for (i in 0 until(destinations.size())) {
                    val destinationConfig = destinations.getMap(i)
                    val destination = Parser.parseDestination(destinationConfig, carContext)
                    val destinationTravelEstimate = Parser.parseTravelEstimate(destinationConfig.getMap("destinationTravelEstimate")!!)
                    addDestination(destination, destinationTravelEstimate)
                }
            }
        }.build()

        navigationManager.updateTrip(trip)
    }
}