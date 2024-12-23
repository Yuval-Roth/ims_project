package com.imsproject.watch.viewmodel

import android.content.Intent
import android.util.Log
import androidx.compose.runtime.mutableFloatStateOf
import com.imsproject.common.gameServer.GameAction
import com.imsproject.common.gameServer.GameRequest
import com.imsproject.common.gameServer.GameType
import com.imsproject.watch.MY_RADIUS_OUTER_EDGE
import com.imsproject.watch.SCREEN_CENTER
import com.imsproject.watch.UNDEFINED_ANGLE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import com.imsproject.watch.ARC_DEFAULT_ALPHA
import com.imsproject.watch.MAX_ANGLE_SKEW
import com.imsproject.watch.MIN_ANGLE_SKEW
import com.imsproject.watch.MY_RADIUS_INNER_EDGE
import com.imsproject.watch.MY_SWEEP_ANGLE
import com.imsproject.watch.model.Position
import kotlin.math.absoluteValue
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

class WineGlassesViewModel() : GameViewModel(GameType.WINE_GLASSES) {

    class Arc{
        var startAngle = mutableFloatStateOf(UNDEFINED_ANGLE)
        var angleSkew = 0f
        var direction = 0f
        var previousAngle = mutableFloatStateOf(UNDEFINED_ANGLE)
        var previousAngleDiff = 0f
        var currentAlpha = mutableFloatStateOf(ARC_DEFAULT_ALPHA)
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

    private var _released = MutableStateFlow(false)
    val released : StateFlow<Boolean> = _released

    private var _opponentReleased = MutableStateFlow(false)
    val opponentReleased : StateFlow<Boolean> = _opponentReleased

    // ================================================================================ |
    // ============================ PUBLIC METHODS ==================================== |
    // ================================================================================ |

    override fun onCreate(intent: Intent) {
        super.onCreate(intent)
    }

    fun setTouchPoint(x: Double, y: Double) {
        val rawAngle = calculateAngle(x, y)
        val distance = calculateDistance(x, y)

        val inBounds = MY_RADIUS_INNER_EDGE <= distance && distance <= MY_RADIUS_OUTER_EDGE
        if(inBounds){
            updateMyArc(rawAngle)
            _released.value = false
        } else {
            _released.value = true
        }

        val released = released.value
        val angle = myArc.startAngle.floatValue

        // send position to server
        viewModelScope.launch(Dispatchers.IO) {
            model.sendPosition(Angle(angle, released),getCurrentGameTime())
        }
    }

    fun setReleased() {
        _released.value = true

        val angle = myArc.startAngle.floatValue
        // send position to server
        viewModelScope.launch(Dispatchers.IO) {
            model.sendPosition(Angle(angle, true),getCurrentGameTime())
        }
    }

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

                opponentArc.startAngle.floatValue = position.angle
                _opponentReleased.value = position.released
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

    private fun calculateAngle(x: Double, y:Double) : Float {
        return Math.toDegrees(
            atan2(
                y - SCREEN_CENTER.y,
                x - SCREEN_CENTER.x
            ).toDouble()
        ).toFloat()
    }

    private fun calculateDistance(x: Double, y: Double) : Double {
        return sqrt(
            (x - SCREEN_CENTER.x).pow(2) + (y - SCREEN_CENTER.y).pow(2)
        )
    }

    private fun updateMyArc(angle: Float){

        // =========== for current iteration =============== |

        // calculate the skew angle to show the arc ahead of the finger
        // based on the calculations of the previous iteration
        val angleSkew = myArc.angleSkew
        myArc.startAngle.floatValue = angle + myArc.direction * angleSkew - MY_SWEEP_ANGLE / 2

        // ============== for next iteration =============== |

        // prepare the skew angle for the next iteration
        val previousAngle = myArc.previousAngle.floatValue
        val angleDiff = (angle - previousAngle).absoluteValue
        if(previousAngle != UNDEFINED_ANGLE){
            val previousAngleDiff = myArc.previousAngleDiff
            val angleDiffDiff = angleDiff - previousAngleDiff
            myArc.angleSkew = if (angleDiffDiff > 1 && angleDiff > 2){
                (angleSkew + 5f).coerceAtMost(MAX_ANGLE_SKEW)
            } else if (angleDiffDiff < 1){
                (angleSkew - 2.5f).coerceAtLeast(MIN_ANGLE_SKEW)
            } else {
                angleSkew
            }
        }

        // prepare the direction for the next iteration
        if (previousAngle != UNDEFINED_ANGLE && angleDiff > 2){
            val direction = myArc.direction
            myArc.direction = if(angle > previousAngle){
                (direction + 0.1f).coerceAtMost(1f)
            } else if (angle < previousAngle){
                (direction - 0.1f).coerceAtLeast(-1f)
            } else {
                direction
            }
            if(myArc.direction == 0f) myArc.angleSkew = MIN_ANGLE_SKEW
        }

        // current angle becomes previous angle for the next iteration
        myArc.previousAngle.floatValue = angle
        myArc.previousAngleDiff = angleDiff
    }

    companion object {
        private const val TAG = "WineGlassesViewModel"
    }
}