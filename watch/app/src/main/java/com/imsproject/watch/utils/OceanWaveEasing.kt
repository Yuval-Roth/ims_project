package com.imsproject.watch.utils

import androidx.compose.animation.core.Easing
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object OceanWaveEasing : Easing {
    override fun transform(fraction: Float): Float {
        return fraction.pow(0.7f) + sin(fraction * Math.PI).toFloat() * (1 - fraction).pow(0.5f) * 0.2f
    }
}
