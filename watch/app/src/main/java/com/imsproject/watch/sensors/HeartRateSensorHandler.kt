package com.imsproject.watch.sensors

import android.content.Context
import android.util.Log
import com.imsproject.common.gameserver.SessionEvent
import com.imsproject.watch.viewmodel.GameViewModel
import com.samsung.android.service.health.tracking.ConnectionListener
import com.samsung.android.service.health.tracking.HealthTracker
import com.samsung.android.service.health.tracking.HealthTrackerException
import com.samsung.android.service.health.tracking.HealthTrackingService
import com.samsung.android.service.health.tracking.data.DataPoint
import com.samsung.android.service.health.tracking.data.HealthTrackerType
import com.samsung.android.service.health.tracking.data.ValueKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class HeartRateSensorHandler private constructor() {
    private var healthService: HealthTrackingService? = null
    private var tracker: HealthTracker? = null
    private var initialized = false
    private var connected = false
    private var gameViewModel : GameViewModel? = null
    var tracking = false
        private set
    private val _heartRate = MutableStateFlow(0)
    val heartRate: StateFlow<Int> = _heartRate
    private val _ibi = MutableStateFlow(0)
    val ibi: StateFlow<Int> = _ibi
    private var notAvailable = false

    fun init() {
        if (notAvailable) {
            return
        }

        if (initialized) {
            throw IllegalStateException("Heart rate sensor already initialized")
        }

        if(!connected) {
            throw IllegalStateException("Heart rate sensor not connected")
        }

        // validate healthService and tracker
        val healthService = healthService ?: throw IllegalStateException("Health service not connected")
        val tracker = healthService.getHealthTracker(HealthTrackerType.HEART_RATE_CONTINUOUS)
            ?: throw IllegalStateException("Heart rate tracking not supported")

        tracker.setEventListener(object : HealthTracker.TrackerEventListener {
            override fun onDataReceived(dataPoints: List<DataPoint>) {
                dataPoints.forEach {
                    val (hr, ibi) = it.toHeartRateData()
                    _heartRate.value = hr
                    _ibi.value = ibi
                    if (! tracking) return
                    val gameViewModel = gameViewModel ?: return
                    val actor = gameViewModel.playerId
                    val timestamp = gameViewModel.getCurrentGameTime()
                    gameViewModel.addEvent(SessionEvent.heartRate(actor, timestamp, hr.toString()))
                    gameViewModel.addEvent(SessionEvent.interBeatInterval(
                            actor,
                            timestamp,
                            ibi.toString()
                        )
                    )
                }
            }
            override fun onFlushCompleted() = Unit
            override fun onError(error: HealthTracker.TrackerError) {
                Log.e(TAG, "Error: $error")
            }
        })
        this.tracker = tracker
        initialized = true
    }

    fun connect(context: Context, onConnectionResponse: (Boolean, HealthTrackerException?) -> Unit) {

        if (connected) {
            throw IllegalStateException("Already connected to health service")
        }

        val healthService = HealthTrackingService(
            object : ConnectionListener {
                override fun onConnectionSuccess(){
                    connected = true
                    notAvailable = false
                    onConnectionResponse(true,null)
                }
                override fun onConnectionFailed(e: HealthTrackerException) {
                    healthService = null
                    notAvailable = true
                    onConnectionResponse(false,e)
                }
                override fun onConnectionEnded() {}
            }, context)
        this.healthService = healthService
        healthService.connectService()
    }

    fun disconnect() {
        if(notAvailable) {
            return
        }

        if (!connected) {
            throw IllegalStateException("Not connected to health service")
        }
        tracker?.unsetEventListener()
        healthService?.disconnectService()
        tracker = null
        healthService = null
        connected = false
    }

    fun startTracking(gameViewModel: GameViewModel) {
        if(notAvailable) {
            return
        }

        if (!initialized) {
            throw IllegalStateException("Heart rate sensor not initialized")
        }
        if (tracking) {
            throw IllegalStateException("Heart rate sensor already tracking")
        }
        this.gameViewModel = gameViewModel
        tracking = true
    }

    fun stopTracking() {
        if(notAvailable) {
            return
        }

        if (!initialized) {
            throw IllegalStateException("Heart rate sensor not initialized")
        }
        if (!tracking) {
            throw IllegalStateException("Heart rate sensor not tracking")
        }
        this.gameViewModel = null
        tracking = false
    }

    private fun DataPoint.toHeartRateData() =
        getValue(ValueKey.HeartRateSet.HEART_RATE) to
                (getValue(ValueKey.HeartRateSet.IBI_LIST).lastOrNull() ?: 0)

    companion object {
        private const val TAG = "HeartRateSensorHandler"
        val instance: HeartRateSensorHandler = HeartRateSensorHandler()
    }
}