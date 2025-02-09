package com.imsproject.watch.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.imsproject.common.gameserver.SessionEvent
import com.imsproject.watch.viewmodel.GameViewModel

class LocationSensorsHandler(
    context: Context,
    gameViewModel: GameViewModel
) {

    private val sensorManager = context.getSystemService(SensorManager::class.java)
        ?: throw Exception("Sensor manager not found")

    private val gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        ?: throw Exception("Gyroscope sensor not found")

    private val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        ?: throw Exception("Accelerometer sensor not found")

    private val gyroscopeListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val (x,y,z) = event.values
            gameViewModel.addEvent(
                SessionEvent.gyroscope(
                    gameViewModel.playerId,
                    gameViewModel.getCurrentGameTime(),
                    "$x,$y,$z"
                )
            )
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private val accelerometerListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val (x,y,z) = event.values
            gameViewModel.addEvent(
                SessionEvent.accelerometer(
                    gameViewModel.playerId,
                    gameViewModel.getCurrentGameTime(),
                    "$x,$y,$z"
                )
            )
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    fun start() {
        sensorManager.registerListener(
            gyroscopeListener,
            gyroscopeSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        sensorManager.registerListener(
            accelerometerListener,
            accelerometerSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    fun stop() {
        sensorManager.unregisterListener(gyroscopeListener)
        sensorManager.unregisterListener(accelerometerListener)
    }
}