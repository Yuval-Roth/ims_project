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

class HeartRateSensorHandler private constructor() {
    private var healthService: HealthTrackingService? = null
    private var tracker: HealthTracker? = null
    private var initialized = false
    private var connected = false
    var tracking = false
        private set
    private var gameViewModel : GameViewModel? = null

    fun init() {

        if (initialized) {
            throw IllegalStateException("Heart rate sensor already initialized")
        }

        // validate healthService and tracker
        val healthService = healthService ?: throw IllegalStateException("Health service not connected")
        val tracker = healthService.getHealthTracker(HealthTrackerType.HEART_RATE_CONTINUOUS)
            ?: throw IllegalStateException("Heart rate tracking not supported")

        tracker.setEventListener(object : HealthTracker.TrackerEventListener {
            override fun onDataReceived(dataPoints: List<DataPoint>) {
                if (! tracking) return
                val gameViewModel = gameViewModel ?: return
                dataPoints.forEach {
                    val (hr, ibi) = it.toHeartRateData()
                    val actor = gameViewModel.playerId
                    val timestamp = gameViewModel.getCurrentGameTime()
                    gameViewModel.addEvent(SessionEvent.heartRate(actor, timestamp, hr.toString()))
                    gameViewModel.addEvent(
                        SessionEvent.interBeatInterval(
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
    }

    fun connect(context: Context, onConnectionResponse: (Boolean, HealthTrackerException?) -> Unit) {

        if (connected) {
            throw IllegalStateException("Already connected to health service")
        }

        val healthService = HealthTrackingService(
            object : ConnectionListener {
                override fun onConnectionSuccess() = onConnectionResponse(true,null)
                override fun onConnectionFailed(e: HealthTrackerException) {
                    healthService = null
                    onConnectionResponse(false,e)
                }
                override fun onConnectionEnded() {}
            }, context)
        this.healthService = healthService
        healthService.connectService()
        this.connected = true
    }

    fun disconnect() {
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