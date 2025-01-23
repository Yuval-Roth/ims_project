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
    private var spO2Listener: SpO2Listener? = null
    private var previousStatus = SpO2Status.INITIAL_STATUS
    private var heartRateDataLast = HeartRateData()
    private val handler = Handler(Looper.getMainLooper())
    private var connected = false


    private val trackerDataObserver = object : TrackerDataObserver {
        override fun onHeartRateTrackerDataChanged(hrData: HeartRateData) {
            handler.post {
                heartRateDataLast = hrData
                if (hrData.status == HeartRateStatus.HR_STATUS_FIND_HR) {
                    gameViewModel.addEvent(
                        SessionEvent.heartRate(
                            gameViewModel.playerId,
                            gameViewModel.getCurrentGameTime(),
                            "${hrData.hr};${hrData.ibi}"
                        )
                    )
                } else {
                    gameViewModel.addEvent(
                        SessionEvent.heartRate(
                            gameViewModel.playerId,
                            gameViewModel.getCurrentGameTime(),
                            "no data"
                        )
                    )
                }
            }
        }

        override fun onSpO2TrackerDataChanged(status: Int, spO2Value: Int) {
            if (status == previousStatus) return
            previousStatus = status
            when (status) {
                SpO2Status.CALCULATING -> {
                    handler.post {
                        gameViewModel.addEvent(
                            SessionEvent.bloodOxygen(
                                gameViewModel.playerId,
                                gameViewModel.getCurrentGameTime(),
                                "Calculating"
                            )
                        )
                    }
                }
                SpO2Status.DEVICE_MOVING -> {
                    handler.post {
                        gameViewModel.addEvent(
                            SessionEvent.bloodOxygen(
                                gameViewModel.playerId,
                                gameViewModel.getCurrentGameTime(),
                                "Device moving"
                            )
                        )
                    }
                }
                SpO2Status.LOW_SIGNAL -> {
                    handler.post {
                        gameViewModel.addEvent(
                            SessionEvent.bloodOxygen(
                                gameViewModel.playerId,
                                gameViewModel.getCurrentGameTime(),
                                "Low signal quality"
                            )
                        )
                    }
                }
                SpO2Status.MEASUREMENT_COMPLETED -> {
                    isMeasurementRunning.set(false)
                    spO2Listener?.stopTracker()
                    handler.post {
                        gameViewModel.addEvent(
                            SessionEvent.bloodOxygen(
                                gameViewModel.playerId,
                                gameViewModel.getCurrentGameTime(),
                                spO2Value.toString()
                            )
                        )
                    }
                }
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

            spO2Listener = SpO2Listener()
            heartRateListener = HeartRateListener()

            connectionManager!!.initSpO2(spO2Listener)
            connectionManager!!.initHeartRate(heartRateListener)

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
        } catch (t: Throwable) {
            println("SensorError: Could not connect the ConnectionManager: " + t.message)
        }



        heartRateListener = HeartRateListener()
        spO2Listener = SpO2Listener()

        TrackerDataNotifier.getInstance().addObserver(trackerDataObserver)
        connectionManager!!.initHeartRate(heartRateListener!!)
        connectionManager!!.initSpO2(spO2Listener!!)
    }

    fun stop() {

        if (heartRateListener != null) heartRateListener!!.stopTracker()
        if (spO2Listener != null) spO2Listener!!.stopTracker()
        TrackerDataNotifier.getInstance().removeObserver(trackerDataObserver)
        if (connectionManager != null) {
            connectionManager!!.disconnect()
        }
    }

    fun start() {
        if (!isMeasurementRunning.get()) {
            // Start the measurement process
            previousStatus = SpO2Status.INITIAL_STATUS
            spO2Listener?.startTracker()  // Start measuring SpO2
            heartRateListener?.startTracker()  // Optionally, you can start heart rate tracking too
            isMeasurementRunning.set(true)
        }
    }
}