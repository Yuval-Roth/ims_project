package com.imsproject.watch.utils

import com.imsproject.watch.FREQUENCY_HISTORY_MILLISECONDS

private const val SAMPLES_PER_SECOND = 1000 / 60 // 60 fps

class FrequencyTracker {

    private val samplesHistoryCount : Int = (SAMPLES_PER_SECOND * FREQUENCY_HISTORY_MILLISECONDS).toInt()

    val frequency : Float
        get() = sum / sampleCount.coerceIn(1,samplesHistoryCount)

    private val samples = Array(samplesHistoryCount) {0f}
    private var sum : Float = 0f
    private var lastSampleTime : Long = 0
    private var lastSampleAngle : Float = 0f
    private var index : Int = 0
    private var sampleCount : Int = 0

    fun addSample(angle: Float) {
        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - lastSampleTime
        val angleDiff = calculateAngleDiff(lastSampleAngle,angle)
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
        lastSampleAngle = 0f
        index = 0
        samples.fill(0f)
    }
}