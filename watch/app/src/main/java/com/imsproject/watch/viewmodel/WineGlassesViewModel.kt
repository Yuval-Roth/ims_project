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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.lifecycle.viewModelScope
import com.imsproject.watch.ARC_DEFAULT_ALPHA
import com.imsproject.watch.MY_RADIUS_INNER_EDGE
import com.imsproject.watch.model.Position
import kotlin.math.absoluteValue
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

class WineGlassesViewModel() : GameViewModel(GameType.WINE_GLASSES) {

    class Arc{
        var startAngle = mutableFloatStateOf(0f)
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

    private var _angle = MutableStateFlow(UNDEFINED_ANGLE)
    val angle : StateFlow<Float> = _angle

    private var _inBounds = MutableStateFlow(false)
    val inBounds : StateFlow<Boolean> = _inBounds

    private var _released = MutableStateFlow(false)
    val released : StateFlow<Boolean> = _released

    private var _opponentReleased = MutableStateFlow(false)
    val opponentReleased : StateFlow<Boolean> = _opponentReleased

    // ================================================================================ |
    // ============================ PUBLIC METHODS ==================================== |
    // ================================================================================ |

    fun setTouchPoint(x: Double, y: Double) {
        val angle = calculateAngle(x, y)
        val distance = calculateDistance(x, y)

        val inBounds = MY_RADIUS_INNER_EDGE <= distance && distance <= MY_RADIUS_OUTER_EDGE
        _inBounds.value = inBounds
        if(inBounds){
            _angle.value = angle
            _released.value = false
        } else {
            _released.value = true
        }

        val released = released.value

        // send position to server
        viewModelScope.launch(Dispatchers.IO) {
            model.sendPosition(Angle(angle, released),getCurrentGameTime())
        }
    }

    fun setReleased() {
        val angle = _angle.value
        _released.value = false

        // send position to server
        viewModelScope.launch(Dispatchers.IO) {
            model.sendPosition(Angle(angle, false),getCurrentGameTime())
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

    companion object {
        private const val TAG = "WineGlassesViewModel"
    }
}