package com.imsproject.watch.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.imsproject.common.gameserver.SessionEvent
import com.imsproject.watch.viewmodel.GameViewModel

class LocationSensorsHandler(
    val context: Context,
    val gameViewModel: GameViewModel
) {

    // Sensor Manager and sensor references
    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var gyroscopeSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    private var gyroscopeListener: SensorEventListener? = null
    private var accelerometerListener: SensorEventListener? = null

    init {
        // Initialize the gyroscope and accelerometer sensors
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Create listener for gyroscope
        gyroscopeListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null && event.sensor.type == Sensor.TYPE_GYROSCOPE) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    gameViewModel.addEvent(
                        SessionEvent.gyroscope(
                            gameViewModel.playerId,
                            gameViewModel.getCurrentGameTime(),
                            "$x,$y,$z"
                        )
                    )
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        // Create listener for accelerometer
        accelerometerListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null && event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    gameViewModel.addEvent(
                        SessionEvent.accelerometer(
                            gameViewModel.playerId,
                            gameViewModel.getCurrentGameTime(),
                            "$x,$y,$z"
                        )
                    )
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }

    // Method to start listening to the sensors
    fun start() {
        gyroscopeSensor?.let {
            sensorManager.registerListener(
                gyroscopeListener,
                it,
                SensorManager.SENSOR_DELAY_NORMAL // Use a slower sampling rate
            )
        }
        accelerometerSensor?.let {
            sensorManager.registerListener(
                accelerometerListener,
                it,
                SensorManager.SENSOR_DELAY_NORMAL // Use a slower sampling rate
            )
        }
    }

    // Method to stop listening to the sensors
    fun stop() {
        sensorManager.unregisterListener(gyroscopeListener)
        sensorManager.unregisterListener(accelerometerListener)
    }
}