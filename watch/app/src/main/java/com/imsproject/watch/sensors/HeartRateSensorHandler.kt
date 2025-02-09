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

class HeartRateSensorHandler() {
    private var healthService: HealthTrackingService? = null
    private var tracker: HealthTracker? = null

    fun connect(context: Context, onConnectionResponse: (Boolean, HealthTrackerException?) -> Unit) {
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
    }

    fun disconnect() {
        tracker?.unsetEventListener()
        healthService?.disconnectService()
        tracker = null
        healthService = null
    }

    fun startTracking(gameViewModel: GameViewModel) {

        // validate healthService and tracker
        val healthService = healthService ?: throw IllegalStateException("Health service not connected")
        val tracker = healthService.getHealthTracker(HealthTrackerType.HEART_RATE_CONTINUOUS)
            ?: throw IllegalStateException("Heart rate tracking not supported")

        tracker.setEventListener(object : HealthTracker.TrackerEventListener {
            override fun onDataReceived(dataPoints: List<DataPoint>) = dataPoints.forEach {
                val (hr,ibi) = it.toHeartRateData()
                gameViewModel.addEvent(
                    SessionEvent.heartRate(
                        gameViewModel.playerId,
                        gameViewModel.getCurrentGameTime(),
                        "${hr};${ibi}"
                    )
                )
            }
            override fun onFlushCompleted() = Unit
            override fun onError(error: HealthTracker.TrackerError) {
                Log.e(TAG, "Error: $error")
            }
        })
        this.tracker = tracker
    }

    private fun DataPoint.toHeartRateData() =
        getValue(ValueKey.HeartRateSet.HEART_RATE) to
                (getValue(ValueKey.HeartRateSet.IBI_LIST).lastOrNull() ?: 0)

    companion object {
        private const val TAG = "HeartRateMonitor"
    }
}