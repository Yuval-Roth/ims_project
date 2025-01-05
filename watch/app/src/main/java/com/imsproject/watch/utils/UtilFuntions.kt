package com.imsproject.watch.utils

import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import com.imsproject.watch.SCREEN_CENTER
import kotlin.math.absoluteValue
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

fun Float.isBetweenInclusive(min: Float, max: Float) =  min <= this && this <= max

/**
 * this function assumes that the angle is in the range of (-180,180]
 * and the quadrant is in the range of [1,4]
 *
 * the quadrant are defined as follows:
 * 1. 0 < angle <= 90
 * 2. 90 < angle <= 180
 * 3. -180 < angle <= -90
 * 4. -90 < angle <= 0
 *
 * meaning, clockwise side of the quadrant is inclusive and the counter-clockwise side is exclusive
 */
fun Float.isInQuadrant(@IntRange(1,4) quadrant: Int) : Boolean {
    return when(quadrant){
        1 -> 0f < this && this <= 90f
        2 -> 90f < this && this <= 180f
        3 -> -180f < this && this <= -90f
        4 -> -90f < this && this <= 0f
        else -> throw IllegalArgumentException("Invalid quadrant: $quadrant")
    }
}

/**
 * @return Pair of `<distance,angle>` where angle in the range of (-180,180]
 */
fun cartesianToPolar(x: Double, y:Double) : Pair<Float,Float> {
    val distance = sqrt(
        (x - SCREEN_CENTER.x).pow(2) + (y - SCREEN_CENTER.y).pow(2)
    ).toFloat()
    val angle = Math.toDegrees(
        atan2(
            y - SCREEN_CENTER.y,
            x - SCREEN_CENTER.x
        ).toDouble()
    ).toFloat()
    return distance to angle
}

/**
 * This function assumes that the angles are in the range of (-180,180]
 * @return Pair of `<x,y>`
 */
fun polarToCartesian(
    distanceFromCenter: Float,
    @FloatRange(-180.0,180.0,false,true) angle: Float
) : Pair<Float, Float> {
    // Convert angle to radians
    val angleRadians = Math.toRadians(angle.toDouble())

    // Calculate coordinates
    val (centerX, centerY) = SCREEN_CENTER
    val x = centerX + distanceFromCenter * cos(angleRadians)
    val y = centerY + distanceFromCenter * sin(angleRadians)

    return x.toFloat() to y.toFloat()
}

/**
 * This function assumes that the angles are in the range of (-180,180]
 */
fun calculateAngleDiff(
    @FloatRange(-180.0,180.0,false,true) angle1: Float,
    @FloatRange(-180.0,180.0,false,true) angle2: Float
) : Float {
    // handle the gap between 2nd and 3rd quadrants
    val diff = if(angle1.isInQuadrant(2) && angle2.isInQuadrant(3)){
        angle1 - (angle2+360)
    }
    else if(angle1.isInQuadrant(3) && angle2.isInQuadrant(2)){
        (angle1+360) - angle2
    }
    // simple case
    else {
        angle1 - angle2
    }
    return diff.absoluteValue
}

/**
 * This function assumes that the angles are in the range of (-180,180]
 */
fun isClockwise(
    @FloatRange(-180.0,180.0,false,true) previousAngle: Float,
    @FloatRange(-180.0,180.0,false,true) newAngle: Float
) : Boolean {
    //handle the gap between 2nd and 3rd quadrants
    return if(previousAngle.isInQuadrant(2) && newAngle.isInQuadrant(3)){
        true
    } else if(previousAngle.isInQuadrant(3) && newAngle.isInQuadrant(2)){
        false
    }
    // simple case
    else {
        previousAngle < newAngle
    }
}

/**
 * This function assumes that the angles are in the range of (-180,180]
 * and the addition is in the range of [-180,180]
 */
fun addToAngle(
    @FloatRange(-180.0,180.0,false,true) angle: Float,
    @FloatRange(-180.0,180.0,true,true) addition: Float
) : Float {
    val added = angle + addition

    //handle the gap between 2nd and 3rd quadrants
    if(angle.isInQuadrant(2) && addition > 0){
        return if (added > 180){
            added - 360
        } else {
            added
        }
    } else if(angle.isInQuadrant(3) && addition < 0){
        return if (added <= -180){
            added + 360
        } else {
            added
        }
    }
    // simple case
    else {
        return added
    }
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

fun Float.sign() = if(this < 0) -1 else if (this > 0) 1 else 0


