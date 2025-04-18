package com.imsproject.common.utils

import kotlin.math.absoluteValue

const val UNDEFINED_ANGLE = 600f

/**
 * This class represents an angle in degrees in the range of (-180,180]
 *
 * The constructor also accepts a special value for undefined angle which is defined by [UNDEFINED_ANGLE]
 *
 * The [plus] and [minus] operators are overloaded to support two operations:
 * 1. [plus] allows adding a positive or negative number (or an `Angle` object) to the current angle, resulting in a new angle
 * 2. [minus] allows calculating the absolute difference between two angles
 */
class Angle(
    val floatValue: Float
) {

    init {
        if(floatValue != UNDEFINED_ANGLE && (floatValue <= -180 || floatValue > 180)){
            throw IllegalArgumentException("Invalid angle value: $floatValue")
        }
    }

    val doubleValue: Double
        get() = floatValue.toDouble()

    /**
     * @return absolute difference between this angle and the other angle
     */
    operator fun minus(other: Angle): Float {
        // handle the gap between 2nd and 3rd quadrants
        val diff = if(this.isInQuadrant(2) && other.isInQuadrant(3)){
            this.floatValue - (other.floatValue+360)
        }
        else if(this.isInQuadrant(3) && other.isInQuadrant(2)){
            (this.floatValue+360) - other.floatValue
        }
        // simple case
        else {
            this.floatValue - other.floatValue
        }
        return diff.absoluteValue
    }

    /**
     * @return a new angle that is the sum of this angle and the other angle
     */
    operator fun plus(other: Float): Angle {
        var added = this.floatValue + other

        //handle the gap between 2nd and 3rd quadrants
        added = if(this.isInQuadrant(2) && other > 0){
            if (added > 180){
                added - 360
            } else {
                added
            }
        } else if(this.isInQuadrant(3) && other < 0){
            if (added <= -180){
                added + 360
            } else {
                added
            }
        }
        // simple case
        else {
            added
        }
        return Angle(added)
    }

    /**
     * @return a new angle that is the sum of this angle and the other angle
     */
    operator fun plus(other: Angle): Angle {
        return plus(other.floatValue)
    }

    /**
     * @return a new angle that is the sum of this angle and the other angle
     */
    operator fun plus(other: Number): Angle {
        return plus(other.toFloat())
    }

    fun isBetweenInclusive(min: Float, max: Float) =  min <= floatValue && floatValue <= max

    override fun toString(): String {
        return floatValue.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Angle

        return floatValue == other.floatValue
    }

    override fun hashCode(): Int {
        return floatValue.hashCode()
    }


    private fun isInQuadrant(quadrant: Int) : Boolean {
        return floatValue.isInQuadrant(quadrant)
    }

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
    private fun Float.isInQuadrant(quadrant: Int) : Boolean {
        return when(quadrant){
            1 -> 0f < this && this <= 90f
            2 -> 90f < this && this <= 180f
            3 -> -180f < this && this <= -90f
            4 -> -90f < this && this <= 0f
            else -> throw IllegalArgumentException("Invalid quadrant: $quadrant")
        }
    }

    companion object {
        /**
         * This function assumes that the angles are in the range of (-180,180]
         * and that the difference between the angles is less than 180 degrees
         */
        fun isClockwise(
            from: Angle,
            to: Angle
        ) : Boolean {
            //handle the gap between 2nd and 3rd quadrants
            return if(from.isInQuadrant(2) && to.isInQuadrant(3)){
                true
            } else if(from.isInQuadrant(3) && to.isInQuadrant(2)){
                false
            }
            // simple case
            else {
                from.floatValue < to.floatValue
            }
        }

        val undefined: Angle = Angle(UNDEFINED_ANGLE)
    }
}