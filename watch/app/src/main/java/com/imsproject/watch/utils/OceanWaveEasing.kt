package com.imsproject.watch.utils

import androidx.compose.animation.core.Easing
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object OceanWaveEasing : Easing {
    override fun transform(fraction: Float): Float {
//        println(fraction)
//        val t = fraction.coerceIn(0f, 1f).toDouble()
//        val a = 4f
//        val denom = 1.0 - exp(-a)
//        val y = (1.0 - exp(-a * t)) / denom
//        return y.toFloat().coerceIn(0f, 1f)
        return fraction.pow(0.4f) + sin(fraction * Math.PI).toFloat() * (1 - fraction).pow(2f) * 0.3f
    }
}
