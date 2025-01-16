package com.imsproject.watch.sensors

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.imsproject.common.gameserver.SessionEvent
import com.imsproject.watch.viewmodel.GameViewModel
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.samsung.android.service.health.tracking.HealthTrackerException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class SensorsHandler(
    val scope: CoroutineScope,
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
    private val measurementProgress: CircularProgressIndicator

    private val trackerDataObserver = object : TrackerDataObserver {
        override fun onHeartRateTrackerDataChanged(hrData: HeartRateData) {
            handler.post {
                heartRateDataLast = hrData
                if (hrData.status == HeartRateStatus.HR_STATUS_FIND_HR) {
                    gameViewModel.addEvent(
                        SessionEvent.heartRate(
                            gameViewModel.playerId,
                            gameViewModel.getCurrentGameTime(),
                            hrData.hr,
                            hrData.ibi
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
                                spO2Value
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

        measurementProgress = CircularProgressIndicator(context).apply {
            isIndeterminate = false
            max = 100  // Just a placeholder; won't be used since we're no longer counting time
        }

        heartRateListener = HeartRateListener()
        spO2Listener = SpO2Listener()

        TrackerDataNotifier.getInstance().addObserver(trackerDataObserver)
        connectionManager.initHeartRate(heartRateListener!!)
        connectionManager.initSpO2(spO2Listener!!)
    }

    fun cleanup() {
        heartRateListener?.stopTracker()
        spO2Listener?.stopTracker()
        TrackerDataNotifier.getInstance().removeObserver(trackerDataObserver)
        connectionManager.disconnect()
    }

    fun run() {
        scope.launch(Dispatchers.IO) {
            if (!isMeasurementRunning.get()) {
                // Start the measurement process
                previousStatus = SpO2Status.INITIAL_STATUS
                measurementProgress.progress = 0
                spO2Listener?.startTracker()  // Start measuring SpO2
                heartRateListener?.startTracker()  // Optionally, you can start heart rate tracking too
                isMeasurementRunning.set(true)
            }
        }
    }

    override fun onConnectionResult(stringResourceId: Int) {
        // Handle connection result
    }

    override fun onError(e: HealthTrackerException?) {
        // Handle errors
    }
}