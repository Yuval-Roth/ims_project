package com.imsproject.watch.utils

private const val HISTORY_SECONDS = 5
private const val SAMPLES_PER_SECOND = 1000 / 60 // 60 fps
private const val SAMPLES_HISTORY_COUNT : Int = SAMPLES_PER_SECOND * HISTORY_SECONDS

class FrequencyTracker {

    val frequency : Float
        get() = sum / sampleCount.coerceIn(1,SAMPLES_HISTORY_COUNT)

    private val samples = Array<Float>(SAMPLES_HISTORY_COUNT) {0f}
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
        index = (index + 1) % SAMPLES_HISTORY_COUNT
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