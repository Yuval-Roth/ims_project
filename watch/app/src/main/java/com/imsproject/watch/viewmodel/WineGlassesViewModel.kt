package com.imsproject.watch.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.lifecycle.viewModelScope
import com.imsproject.common.gameServer.GameAction
import com.imsproject.common.gameServer.GameRequest
import com.imsproject.common.gameServer.GameType
import com.imsproject.watch.ACTIVITY_DEBUG_MODE
import com.imsproject.watch.UNDEFINED_ANGLE
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.imsproject.watch.ARC_DEFAULT_ALPHA
import com.imsproject.watch.INNER_TOUCH_POINT
import com.imsproject.watch.MAX_ANGLE_SKEW
import com.imsproject.watch.MIN_ANGLE_SKEW
import com.imsproject.watch.MY_SWEEP_ANGLE
import com.imsproject.watch.OUTER_TOUCH_POINT
import com.imsproject.watch.WINE_GLASSES_SYNC_FREQUENCY_THRESHOLD
import com.imsproject.watch.model.Position
import com.imsproject.watch.utils.FrequencyTracker
import com.imsproject.watch.utils.addToAngle
import com.imsproject.watch.utils.cartesianToPolar
import com.imsproject.watch.utils.calculateAngleDiff
import com.imsproject.watch.utils.isBetweenInclusive
import com.imsproject.watch.utils.isClockwise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue


private const val DIRECTION_MAX_OFFSET = 1.0f

class WineGlassesViewModel : GameViewModel(GameType.WINE_GLASSES) {

    class Arc{
        var startAngle by mutableFloatStateOf(UNDEFINED_ANGLE)
        var angleSkew = 0f
        var direction = 0f
        var previousAngle by mutableFloatStateOf(UNDEFINED_ANGLE)
        var previousAngleDiff = 0f
        var currentAlpha by mutableFloatStateOf(ARC_DEFAULT_ALPHA)
    }

    class Angle (
        val angle: Float,
        val released: Boolean
    ) : Position{
        override fun toString(): String {
            return "$angle,$released"
        }

        companion object {
            fun fromString(string: String) : Angle {
                val parts = string.split(",")
                return Angle(
                    parts[0].toFloat(),
                    parts[1].toBoolean()
                )
            }
        }
    }

    // ================================================================================ |
    // ================================ STATE FIELDS ================================== |
    // ================================================================================ |

    val myArc = Arc()
    val opponentArc = Arc()
    val myFrequencyTracker = FrequencyTracker()
    val opponentFrequencyTracker = FrequencyTracker()

    private var _released = MutableStateFlow(true)
    val released : StateFlow<Boolean> = _released

    private var _opponentReleased = MutableStateFlow(false)
    val opponentReleased : StateFlow<Boolean> = _opponentReleased

    // ================================================================================ |
    // ============================ PUBLIC METHODS ==================================== |
    // ================================================================================ |

    override fun onCreate(intent: Intent, context: Context) {
        super.onCreate(intent,context)

        if(ACTIVITY_DEBUG_MODE){
            viewModelScope.launch(Dispatchers.IO) {
                while(true) {
                    var angle = 0.0f
                    while(angle < 360 * 15){
                        opponentArc.startAngle = angle
                        opponentFrequencyTracker.addSample(angle)
                        angle += 4
                        delay(16)
                    }
                    _opponentReleased.value = true
                    opponentFrequencyTracker.reset()
                    delay(2000)
                    _opponentReleased.value = false
                }
            }
        }
    }

    fun setTouchPoint(x: Double, y: Double) {
        val (distance,rawAngle) = cartesianToPolar(x, y)
        val inBounds = distance in INNER_TOUCH_POINT..OUTER_TOUCH_POINT
        if(inBounds){
            updateMyArc(rawAngle)
            _released.value = false
            myFrequencyTracker.addSample(myArc.startAngle)
        } else {
            _released.value = true
            myFrequencyTracker.reset()
        }

        val released = released.value
        val angle = myArc.startAngle

        if(ACTIVITY_DEBUG_MODE) return
        // send position to server
        viewModelScope.launch(Dispatchers.IO) {
            model.sendPosition(Angle(angle, released),getCurrentGameTime())
        }
    }

    fun setReleased() {
        _released.value = true
        myFrequencyTracker.reset()
        val angle = myArc.startAngle

        if(ACTIVITY_DEBUG_MODE) return
        // send position to server
        viewModelScope.launch(Dispatchers.IO) {
            model.sendPosition(Angle(angle, true),getCurrentGameTime())
        }
    }

    fun inSync() = !released.value && !opponentReleased.value
        && (myFrequencyTracker.frequency - opponentFrequencyTracker.frequency)
            .absoluteValue < WINE_GLASSES_SYNC_FREQUENCY_THRESHOLD


    // ================================================================================ |
    // ============================ PRIVATE METHODS =================================== |
    // ================================================================================ |

    /**
     * handles game actions
     */
    override suspend fun handleGameAction(action: GameAction) {
        when (action.type) {
            GameAction.Type.POSITION -> {
                val actor = action.actor ?: run{
                    Log.e(TAG, "handleGameAction: missing actor in position action")
                    return
                }
                val timestamp = action.timestamp?.toLong() ?: run{
                    Log.e(TAG, "handleGameAction: missing timestamp in position action")
                    return
                }
                val position = action.data?.let { Angle.fromString(it) } ?: run{
                    Log.e(TAG, "handleGameAction: missing position in position action")
                    return
                }

                opponentArc.startAngle = position.angle
                _opponentReleased.value = position.released
                if(position.released){
                    opponentFrequencyTracker.reset()
                } else {
                    opponentFrequencyTracker.addSample(position.angle)
                }
            }
            else -> super.handleGameAction(action)
        }
    }

    /**
     * handles game requests
     */
    override suspend fun handleGameRequest(request: GameRequest) {
        when (request.type) {
            GameRequest.Type.END_GAME -> exitOk()
            else -> super.handleGameRequest(request)
        }
    }

    private fun updateMyArc(angle: Float){

        // =========== for current iteration =============== |

        // calculate the skew angle to show the arc ahead of the finger
        // based on the calculations of the previous iteration
        val angleSkew = myArc.angleSkew
        myArc.startAngle = addToAngle(angle,
                myArc.direction.coerceIn(-DIRECTION_MAX_OFFSET, DIRECTION_MAX_OFFSET) * angleSkew
                - MY_SWEEP_ANGLE / 2
        )

        // ============== for next iteration =============== |

        // prepare the skew angle for the next iteration
        val previousAngle = myArc.previousAngle
        var angleDiff = 0f
        if(previousAngle != UNDEFINED_ANGLE){
            angleDiff = calculateAngleDiff(previousAngle, angle)
            val previousAngleDiff = myArc.previousAngleDiff
            val angleDiffDiff = angleDiff - previousAngleDiff
            myArc.angleSkew = if (angleDiffDiff > 1 && angleDiff > 3){
                (angleSkew + angleDiff * 0.75f).coerceAtMost(MAX_ANGLE_SKEW)
            } else if (angleDiffDiff < 1){
                (angleSkew - angleDiff * 0.375f).coerceAtLeast(MIN_ANGLE_SKEW)
            } else {
                angleSkew
            }
        }

        // prepare the direction for the next iteration
        // we add a bit to the max offset to prevent random jitter in the direction
        // we clamp the direction to the max offset when calculating the skewed angle
        if (previousAngle != UNDEFINED_ANGLE){
            val direction = myArc.direction
            myArc.direction = if(isClockwise(previousAngle, angle)){
                (direction + angleDiff * 0.2f).coerceAtMost(DIRECTION_MAX_OFFSET + 0.5f)
            } else if (! isClockwise(previousAngle, angle)){
                (direction - angleDiff * 0.2f).coerceAtLeast(-(DIRECTION_MAX_OFFSET + 0.5f))
            } else {
                direction
            }
            if(myArc.direction.isBetweenInclusive(-WINE_GLASSES_SYNC_FREQUENCY_THRESHOLD,
                    WINE_GLASSES_SYNC_FREQUENCY_THRESHOLD
                )) myArc.angleSkew = MIN_ANGLE_SKEW
        }

        // current angle becomes previous angle for the next iteration
        myArc.previousAngle = angle
        myArc.previousAngleDiff = angleDiff
    }

    companion object {
        private const val TAG = "WineGlassesViewModel"
    }
}