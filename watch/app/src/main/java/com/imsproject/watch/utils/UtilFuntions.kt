package com.imsproject.watch.utils

import com.imsproject.common.utils.Angle
import com.imsproject.watch.SCREEN_CENTER
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * @return Pair of `<distance,angle>` where angle in the range of (-180,180]
 */
fun cartesianToPolar(x: Float, y:Float) : Pair<Float,Angle> {
    val distance = sqrt((x - SCREEN_CENTER.x).pow(2) + (y - SCREEN_CENTER.y).pow(2))
    val angle = Math.toDegrees(
        atan2(
            y - SCREEN_CENTER.y,
            x - SCREEN_CENTER.x
        ).toDouble()
    ).toFloat()
    return distance to angle.toAngle()
}

/**
 * This function assumes that the angles are in the range of (-180,180]
 * @return Pair of `<x,y>`
 */
fun polarToCartesian(distanceFromCenter: Float, angle: Angle) : Pair<Float, Float> {
    return polarToCartesian(distanceFromCenter,angle.doubleValue)
}

/**
 * This function assumes that the angles are in the range of (-180,180]
 * @return Pair of `<x,y>`
 */
fun polarToCartesian(distanceFromCenter: Float, angle: Double) : Pair<Float, Float> {
    // Convert angle to radians
    val angleRadians = Math.toRadians(angle)

    // Calculate coordinates
    val (centerX, centerY) = SCREEN_CENTER
    val x = centerX + distanceFromCenter * cos(angleRadians)
    val y = centerY + distanceFromCenter * sin(angleRadians)

    return x.toFloat() to y.toFloat()
}

@Suppress("NOTHING_TO_INLINE")
inline fun Float.isBetweenInclusive(min: Float, max: Float) = min <= this && this <= max

@Suppress("NOTHING_TO_INLINE")
inline fun Float.sign() = if(this < 0) -1 else if (this > 0) 1 else 0

@Suppress("NOTHING_TO_INLINE")
inline fun Int.fastCoerceAtLeast(min: Int) = if (this < min) min else this

@Suppress("NOTHING_TO_INLINE")
inline fun Int.fastCoerceAtMost(max: Int) = if (this > max) max else this

@Suppress("NOTHING_TO_INLINE")
inline fun Int.fastCoerceIn(min: Int, max: Int) = when {
    this < min -> min
    this > max -> max
    else -> this
}

@Suppress("NOTHING_TO_INLINE")
inline fun Float.toAngle() = Angle(this)

@Suppress("NOTHING_TO_INLINE")
inline fun ClosedFloatingPointRange<Float>.random() = Random.nextFloat() * (endInclusive - start) + start

@Suppress("NOTHING_TO_INLINE")
inline fun closestQuantizedAngle(targetAngle:Float, step:Float, quantizedAngles: Array<Float>) : Float {
    return quantizedAngles[(targetAngle / step).roundToInt()]
}

/**
 * Generate quantized angles from 0 (inclusive) to 360 with the given step.
 */
fun quantizeAngles(step: Float): Array<Float>{
    val angles = mutableListOf<Float>()
    var current = 0f
    while(current <= 360f){
        angles.add(current)
        current = current + step
    }
    return angles.toTypedArray()
}

