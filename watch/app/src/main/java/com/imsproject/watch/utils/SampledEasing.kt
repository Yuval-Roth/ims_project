package com.imsproject.watch.utils

import androidx.compose.animation.core.Easing

/**
 * Linearly interpolates over a user-provided set of samples for fraction ∈ [0, 1].
 * The curve shape is defined entirely by [sampleValues].
 */
class SampledEasing(private val sampleValues: FloatArray) : Easing {
    constructor(samples: List<Float>) : this(samples.toFloatArray())

    init {
        require(sampleValues.size == 100) { "sampleValues must contain exactly 100 elements" }
        require(sampleValues.all { it in 0f..1f }) { "All sampleValues must be in the range [0, 1]" }
        require(sampleValues.first() == 0f) { "The first sample value must be 0f" }
        require(sampleValues.last() == 1f) { "The last sample value must be 1f" }
    }

    override fun transform(fraction: Float): Float {
        if (fraction == 0f) return 0f
        if (fraction == 1f) return 1f

        val lowerBoundIndex = (fraction * 100).toInt()
        println("fraction: $fraction, lowerBoundIndex: $lowerBoundIndex")

        // decide on upper bound index
        val threeMostSignificant = (fraction * 1000).toInt()
        if (threeMostSignificant % 10 == 0) {
            // if the third decimal place is zero, we are exactly on a sample point
            // and we don't need to interpolate between two sample points
            return sampleValues[lowerBoundIndex]
        }
        // otherwise, we need to interpolate between two sample points
        val upperBoundIndex = (lowerBoundIndex + 1).coerceAtMost(99)

        // linearly interpolate between the two sample values
        val lowerValue = sampleValues[lowerBoundIndex]
        val upperValue = sampleValues[upperBoundIndex]
        val localFraction = (fraction * 100) - lowerBoundIndex
        val f = lowerValue + (upperValue - lowerValue) * localFraction
        println("f: $f")
        return f
    }
}