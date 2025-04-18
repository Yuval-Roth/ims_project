package com.imsproject.watch.utils

import com.imsproject.common.utils.Angle
import com.imsproject.watch.FREQUENCY_HISTORY_MILLISECONDS

private const val SAMPLES_PER_SECOND = 1000 / 16f // 60 fps

class FrequencyTracker {

    private val samplesHistoryCount : Int = (SAMPLES_PER_SECOND * FREQUENCY_HISTORY_MILLISECONDS / 1000f).toInt()

    val frequency : Float
        get() = (sum / sampleCount.fastCoerceIn(1,samplesHistoryCount)).let { if(it < 0.001) 0f else it }

    private val samples = Array(samplesHistoryCount) {0f}
    private var sum : Float = 0f
    private var lastSampleTime : Long = 0
    private var lastSampleAngle : Angle = 0f.toAngle()
    private var index : Int = 0
    private var sampleCount : Int = 0

    fun addSample(angle: Angle) {
        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - lastSampleTime
        if(timeDiff == 0L) return
        val angleDiff = lastSampleAngle - angle
        val radiansDiff = Math.toRadians(angleDiff.toDouble())
        val omega = radiansDiff / (timeDiff / 1000.0)
        val frequency = (omega / (2 * Math.PI)).toFloat()
        samples[index] = frequency
        sum += frequency
        lastSampleTime = currentTime
        lastSampleAngle = angle
        index = (index + 1) % samplesHistoryCount
        sampleCount++
        sum -= samples[index]
    }

    fun reset() {
        sum = 0f
        lastSampleTime = 0
        lastSampleAngle = 0f.toAngle()
        index = 0
        samples.fill(0f)
    }
}