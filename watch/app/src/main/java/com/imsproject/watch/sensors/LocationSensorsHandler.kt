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

class LocationSensorsHandler private constructor() {

    private lateinit var sensorManager: SensorManager
    private lateinit var magneticFieldSensor: Sensor
    private lateinit var accelerometerSensor: Sensor
    private lateinit var linearAccelerationSensor: Sensor
    private lateinit var orientationListener: SensorEventListener
    private lateinit var linearAccelerationListener: SensorEventListener
    private var initialized = false
    var tracking = false
        private set
    private var gameViewModel: GameViewModel? = null

    fun init(context: Context) {

        if(initialized) {
            throw IllegalStateException("Location sensors already initialized")
        }

        // ======================== Sensors ============================= |
        sensorManager = context.getSystemService(SensorManager::class.java)
            ?: throw Exception("Sensor manager not found")
        magneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            ?: throw Exception("magnetic field sensor not found")
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            ?: throw Exception("Accelerometer sensor not found")
        linearAccelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
            ?: throw Exception("Linear acceleration sensor not found")

        // ======================== Device Orientation ============================= |
        orientationListener = object : SensorEventListener {

            private val accelerometerLastValues = AtomicReference<FloatArray?>(null)
            private val magneticFieldLastValues = AtomicReference<FloatArray?>(null)
            private val counter = AtomicInteger(0)

            override fun onSensorChanged(event: SensorEvent) {
                val gameViewModel = gameViewModel ?: return
                val (x,y,z) = event.values
                when(event.sensor.type) {
                    Sensor.TYPE_MAGNETIC_FIELD -> magneticFieldLastValues.set(floatArrayOf(x,y,z))
                    Sensor.TYPE_ACCELEROMETER -> accelerometerLastValues.set(floatArrayOf(x,y,z))
                }
                if(shouldEmitOrientationEvent()) {
                    emitOrientationEvent(gameViewModel)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

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

        // ======================== Linear Acceleration ============================= |
        linearAccelerationListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val gameViewModel = gameViewModel ?: return
                val (x,y,z) = event.values
                val actor = gameViewModel.playerId
                val timestamp = gameViewModel.getCurrentGameTime()
                gameViewModel.addEvent(SessionEvent.linearAccelerationX(actor, timestamp, x.toString()))
                gameViewModel.addEvent(SessionEvent.linearAccelerationY(actor, timestamp, y.toString()))
                gameViewModel.addEvent(SessionEvent.linearAccelerationZ(actor, timestamp, z.toString()))
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        initialized = true
    }

    fun startTracking(gameViewModel: GameViewModel) {

        if(!initialized) {
            throw IllegalStateException("Location sensors not initialized")
        }

        if(tracking) {
            throw IllegalStateException("Location sensors already tracking")
        }

        sensorManager.registerListener(
            orientationListener,
            magneticFieldSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        sensorManager.registerListener(
            orientationListener,
            accelerometerSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        sensorManager.registerListener(
            linearAccelerationListener,
            linearAccelerationSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        tracking = true
        this.gameViewModel = gameViewModel
    }

    fun stopTracking() {
        if(!initialized) {
            throw IllegalStateException("Location sensors not initialized")
        }
        if(!tracking) {
            throw IllegalStateException("Location sensors not tracking")
        }

        sensorManager.unregisterListener(orientationListener)
        sensorManager.unregisterListener(linearAccelerationListener)
        tracking = false
        gameViewModel = null
    }

    companion object {
        private const val TAG = "LocationSensorsHandler"
        val instance : LocationSensorsHandler = LocationSensorsHandler()
    }
}