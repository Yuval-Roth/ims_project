package com.imsproject.watch.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.imsproject.common.gameserver.SessionEvent
import com.imsproject.watch.viewmodel.GameViewModel
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class LocationSensorsHandler(
    context: Context,
    gameViewModel: GameViewModel
) {

    private val accelerometerLastValues = AtomicReference<FloatArray?>(null)
    private val magneticFieldLastValues = AtomicReference<FloatArray?>(null)
    private val counter = AtomicInteger(0)

    private val sensorManager = context.getSystemService(SensorManager::class.java)
        ?: throw Exception("Sensor manager not found")

    private val magneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        ?: throw Exception("magnetic field sensor not found")

    private val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        ?: throw Exception("Accelerometer sensor not found")

    private val magneticFieldListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val (x,y,z) = event.values
            magneticFieldLastValues.set(floatArrayOf(x,y,z))
            if(shouldEmitOrientationEvent()) {
                emitOrientationEvent(gameViewModel)
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private val accelerometerListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val (x,y,z) = event.values
            accelerometerLastValues.set(floatArrayOf(x,y,z))

            val actor = gameViewModel.playerId
            val timestamp = gameViewModel.getCurrentGameTime()
            gameViewModel.addEvent(SessionEvent.accelerometerX(actor, timestamp, x.toString()))
            gameViewModel.addEvent(SessionEvent.accelerometerY(actor, timestamp, y.toString()))
            gameViewModel.addEvent(SessionEvent.accelerometerZ(actor, timestamp, z.toString()))

            if(shouldEmitOrientationEvent()) {
                emitOrientationEvent(gameViewModel)
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    fun start() {
        sensorManager.registerListener(
            magneticFieldListener,
            magneticFieldSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        sensorManager.registerListener(
            accelerometerListener,
            accelerometerSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    fun stop() {
        sensorManager.unregisterListener(magneticFieldListener)
        sensorManager.unregisterListener(accelerometerListener)
    }

    private fun shouldEmitOrientationEvent(): Boolean {
        var new: Int
        do{
            val current = counter.get()
            new = (current + 1) % 2
        } while(!counter.compareAndSet(current,new))
        return new == 0
    }

    @Suppress("LocalVariableName")
    private fun emitOrientationEvent(gameViewModel: GameViewModel) {
        val magneticFieldValues = magneticFieldLastValues.get() ?: return
        val accelerometerValues = accelerometerLastValues.get() ?: return

        val R = FloatArray(9)
        val I = FloatArray(9)
        val orientation = FloatArray(3)

        SensorManager.getRotationMatrix(R, I, accelerometerValues, magneticFieldValues)
        SensorManager.getOrientation(R, orientation)

        val (azimuth,pitch,roll) = orientation
        val actor = gameViewModel.playerId
        val timestamp = gameViewModel.getCurrentGameTime()
        gameViewModel.addEvent(SessionEvent.orientationAzimuth(actor, timestamp, azimuth.toString()))
        gameViewModel.addEvent(SessionEvent.orientationPitch(actor, timestamp, pitch.toString()))
        gameViewModel.addEvent(SessionEvent.orientationRoll(actor, timestamp, roll.toString()))
    }
}