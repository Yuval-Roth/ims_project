package com.imsproject.watch.sensors

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.imsproject.common.gameserver.SessionEvent
import com.imsproject.watch.viewmodel.GameViewModel
import com.samsung.android.service.health.tracking.HealthTrackerException
import java.util.concurrent.atomic.AtomicBoolean

class SensorsHandler(
    val context: Context,
    val gameViewModel: GameViewModel
) : ConnectionObserver {

    private val isMeasurementRunning = AtomicBoolean(false)
    private var connectionManager: ConnectionManager
    private var heartRateListener: HeartRateListener? = null
    private var spO2Listener: SpO2Listener? = null
    private var previousStatus = SpO2Status.INITIAL_STATUS
    private var heartRateDataLast = HeartRateData()
    private val handler = Handler(Looper.getMainLooper())

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
                // Handle errors if needed
            }
        }
    }

    init {
        // Create instances of connectionManager and measurementProgress in the constructor
        connectionManager = ConnectionManager(this).apply {
            connect(context)
        }

        heartRateListener = HeartRateListener()
        spO2Listener = SpO2Listener()

        TrackerDataNotifier.getInstance().addObserver(trackerDataObserver)
        connectionManager.initHeartRate(heartRateListener!!)
        connectionManager.initSpO2(spO2Listener!!)
    }

    fun stop() {
        heartRateListener?.stopTracker()
        spO2Listener?.stopTracker()
        TrackerDataNotifier.getInstance().removeObserver(trackerDataObserver)
        connectionManager.disconnect()
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

    override fun onConnectionResult(stringResourceId: Int) {
        // Handle connection result
    }

    override fun onError(e: HealthTrackerException?) {
        // Handle errors
    }
}