package com.imsproject.watch.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.fastCoerceIn
import com.imsproject.common.utils.Angle
import com.imsproject.common.utils.UNDEFINED_ANGLE
import com.imsproject.watch.ARC_DEFAULT_ALPHA
import com.imsproject.watch.FLOUR_MILL_SYNC_FREQUENCY_THRESHOLD
import com.imsproject.watch.MARKER_FADE_DURATION
import com.imsproject.watch.MAX_ANGLE_SKEW
import com.imsproject.watch.MIN_ANGLE_SKEW
import com.imsproject.watch.MY_SWEEP_ANGLE
import kotlinx.coroutines.delay

private const val DIRECTION_MAX_OFFSET = 1.0f

class Arc{
    var startAngle by mutableStateOf(Angle.Companion.undefined)
    var angleSkew = 0f
    var direction = 0f
    var previousAngle by mutableStateOf(Angle.Companion.undefined)
    var previousAngleDiff = 0f
    var currentAlpha by mutableFloatStateOf(ARC_DEFAULT_ALPHA)

    fun updateArc(angle: Angle){
        // =========== for current iteration =============== |

        // calculate the skew angle to show the arc ahead of the finger
        // based on the calculations of the previous iteration
        val angleSkew = this.angleSkew
        this.startAngle =
            angle + (this.direction.fastCoerceIn(-DIRECTION_MAX_OFFSET, DIRECTION_MAX_OFFSET)
                    * angleSkew - MY_SWEEP_ANGLE / 2)


        // ============== for next iteration =============== |

        // prepare the skew angle for the next iteration
        val previousAngle = this.previousAngle
        var angleDiff = 0f
        if(previousAngle.floatValue != UNDEFINED_ANGLE){
            angleDiff = previousAngle - angle
            val previousAngleDiff = this.previousAngleDiff
            val angleDiffDiff = angleDiff - previousAngleDiff
            this.angleSkew = if (angleDiffDiff > 1 && angleDiff > 3){
                (angleSkew + angleDiff * 0.75f).fastCoerceAtMost(MAX_ANGLE_SKEW)
            } else if (angleDiffDiff < 1){
                (angleSkew - angleDiff * 0.375f).fastCoerceAtLeast(MIN_ANGLE_SKEW)
            } else {
                angleSkew
            }
        }

        // prepare the direction for the next iteration
        // we add a bit to the max offset to prevent random jitter in the direction
        // we clamp the direction to the max offset when calculating the skewed angle
        if (previousAngle.floatValue != UNDEFINED_ANGLE){
            val direction = this.direction
            this.direction = if(Angle.Companion.isClockwise(previousAngle, angle)){
                (direction + angleDiff * 0.2f).fastCoerceAtMost(DIRECTION_MAX_OFFSET + 0.5f)
            } else if (! Angle.Companion.isClockwise(previousAngle, angle)){
                (direction - angleDiff * 0.2f).fastCoerceAtLeast(-(DIRECTION_MAX_OFFSET + 0.5f))
            } else {
                direction
            }
            if(this.direction.isBetweenInclusive(-FLOUR_MILL_SYNC_FREQUENCY_THRESHOLD,
                    FLOUR_MILL_SYNC_FREQUENCY_THRESHOLD
                )) this.angleSkew = MIN_ANGLE_SKEW
        }

        // current angle becomes previous angle for the next iteration
        this.previousAngle = angle
        this.previousAngleDiff = angleDiff
    }

    suspend fun fadeOut(){
        val alphaAnimStep =  ARC_DEFAULT_ALPHA / (MARKER_FADE_DURATION / 16f)
        while(currentAlpha > 0.0f){
            currentAlpha = (currentAlpha - alphaAnimStep).fastCoerceAtLeast(0.0f)
            delay(16)
        }
        startAngle = Angle.undefined
    }

    fun reset(){
        previousAngle = Angle.undefined
        previousAngleDiff = 0f
        direction = 0f
        angleSkew = MIN_ANGLE_SKEW
    }

    fun show(){
        currentAlpha = ARC_DEFAULT_ALPHA
    }
}