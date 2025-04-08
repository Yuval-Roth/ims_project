package com.imsproject.watch.utils

import com.imsproject.common.utils.Angle
import com.imsproject.watch.SCREEN_CENTER
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

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

/**
 * this function calculates the third point of a triangle given two points and the distance from the second point
 *
 * the distance can be negative to get the point on the opposite side of the second point
 */
fun calculateTriangleThirdPoint(
    p1X: Float, p1Y: Float,
    p2X: Float, p2Y: Float,
    distance: Float
) : Pair<Float, Float> {
    // Direction vector from center to P2
    val directionX = p2X - p1X
    val directionY = p2Y - p1Y

    // Perpendicular vector (-y, x)
    val perpendicularX = -directionY
    val perpendicularY = directionX

    // Normalize the perpendicular vector
    val magnitude = sqrt(perpendicularX * perpendicularX + perpendicularY * perpendicularY)
    val normalizedX = perpendicularX / magnitude
    val normalizedY = perpendicularY / magnitude

    // Scale to the desired distance
    val scaledX = normalizedX * distance
    val scaledY = normalizedY * distance

    // Calculate P3
    val p3X = p2X + scaledX
    val p3Y = p2Y + scaledY

    return p3X to p3Y
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

