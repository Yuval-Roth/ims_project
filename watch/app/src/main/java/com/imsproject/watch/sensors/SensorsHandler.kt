package com.imsproject.watch.sensors

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.imsproject.common.gameserver.SessionEvent
import com.imsproject.watch.R
import com.imsproject.watch.viewmodel.GameViewModel
import com.samsung.android.service.health.tracking.HealthTrackerException
import java.util.concurrent.atomic.AtomicBoolean

class SensorsHandler(
    val context: Context,
    val gameViewModel: GameViewModel
) {

    private val isMeasurementRunning = AtomicBoolean(false)
    private var connectionManager: ConnectionManager? = null
    private var heartRateListener: HeartRateListener? = null
    private var heartRateDataLast = HeartRateData()
    private val handler = Handler(Looper.getMainLooper())
    private var connected = false


    private val trackerDataObserver = object : TrackerDataObserver {
        override fun onHeartRateTrackerDataChanged(hrData: HeartRateData) {
            handler.post {
                heartRateDataLast = hrData
                gameViewModel.addEvent(
                    SessionEvent.heartRate(
                        gameViewModel.playerId,
                        gameViewModel.getCurrentGameTime(),
                        "${hrData.hr};${hrData.ibi}"
                    )
                )
//                if (hrData.status == HeartRateStatus.HR_STATUS_FIND_HR) {
//                    gameViewModel.addEvent(
//                        SessionEvent.heartRate(
//                            gameViewModel.playerId,
//                            gameViewModel.getCurrentGameTime(),
//                            "${hrData.hr};${hrData.ibi}"
//                        )
//                    )
//                } else {
//                    gameViewModel.addEvent(
//                        SessionEvent.heartRate(
//                            gameViewModel.playerId,
//                            gameViewModel.getCurrentGameTime(),
//                            "${hrData.hr};${hrData.ibi}"
//                        )
//                    )
//                }
            }
        }

        override fun onError(errorResourceId: Int) {
            handler.post {
                println("SensorError: tracker data returns error")
                // Handle errors if needed
            }
        }
    }

    private val connectionObserver: ConnectionObserver = object : ConnectionObserver {
        override fun onConnectionResult(stringResourceId: Int) {
            if (stringResourceId != R.string.ConnectedToHs) {
                return
            }

            connected = true
            TrackerDataNotifier.getInstance().addObserver(trackerDataObserver)
            heartRateListener = HeartRateListener()

            var tries = 0
            do{
                try{
                    connectionManager!!.initHeartRate(heartRateListener)
                } catch (e: Exception){
                    Log.e(TAG, "Could not initialize the listeners",e)
                    tries++
                    continue
                }
                break
            } while (tries < 10)

            heartRateListener!!.startTracker()
        }

        override fun onError(e: HealthTrackerException) {
            println("SensorError: Could not connect to Health Tracking Service: " + e.message)
        }
    }

    init {
        // Create instances of connectionManager and measurementProgress in the constructor
        try {
            connectionManager = ConnectionManager(connectionObserver)
            connectionManager!!.connect(context)
            println("SensorError: Connection succeed")
        } catch (t: Throwable) {
            println("SensorError: Could not connect the ConnectionManager: " + t.message)
        }
    }

    fun stop() {

        if (heartRateListener != null) heartRateListener!!.stopTracker()
//        if (spO2Listener != null) spO2Listener!!.stopTracker()
        TrackerDataNotifier.getInstance().removeObserver(trackerDataObserver)
        if (connectionManager != null) {
            connectionManager!!.disconnect()
        }
    }

    fun start() {
        if (!isMeasurementRunning.get() && connected) {
            // Start the measurement process
            heartRateListener?.startTracker()  // Optionally, you can start heart rate tracking too
            isMeasurementRunning.set(true)
        }
        else {
            println("")
        }
    }

    companion object {
        private const val TAG = "SensorsHandler"
    }
}