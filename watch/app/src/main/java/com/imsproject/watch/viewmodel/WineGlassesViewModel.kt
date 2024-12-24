package com.imsproject.watch.viewmodel

import android.content.Intent
import android.util.Log
import androidx.compose.runtime.mutableFloatStateOf
import com.imsproject.common.gameServer.GameAction
import com.imsproject.common.gameServer.GameRequest
import com.imsproject.common.gameServer.GameType
import com.imsproject.watch.UNDEFINED_ANGLE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.lifecycle.viewModelScope
import com.imsproject.watch.ARC_DEFAULT_ALPHA
import com.imsproject.watch.OPPONENT_RADIUS_OUTER_EDGE
import com.imsproject.watch.SCREEN_CENTER
import java.lang.Math.toDegrees
import java.lang.Math.toRadians
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class WineGlassesViewModel() : GameViewModel(GameType.WINE_GLASSES) {

    class Arc{
        var startAngle = mutableFloatStateOf(0f)
        var angleSkew = 0f
        var direction = 0f
        var previousAngle = mutableFloatStateOf(UNDEFINED_ANGLE)
        var previousAngleDiff = 0f
        var currentAlpha = mutableFloatStateOf(ARC_DEFAULT_ALPHA)
    }

    data class Position(
        val x: Double,
        val y: Double,
        val released: Boolean
    ){
        override fun toString(): String {
            return "$x,$y,$released"
        }
        companion object {
            fun fromString(str: String): Position {
                val parts = str.split(",")
                return Position(parts[0].toDouble(), parts[1].toDouble(), parts[2].toBoolean())
            }
        }
    }

    // ================================================================================ |
    // ================================ STATE FIELDS ================================== |
    // ================================================================================ |

    val myArc = Arc()
    val opponentArc = Arc()

    private var _touchPoint = MutableStateFlow(Pair(-1.0,-1.0))
    val touchPoint : StateFlow<Pair<Double,Double>> = _touchPoint

    private var _opponentTouchPoint = MutableStateFlow(Pair(-1.0,-1.0))
    val opponentTouchPoint : StateFlow<Pair<Double,Double>> = _opponentTouchPoint

    private var _released = MutableStateFlow(false)
    val released : StateFlow<Boolean> = _released

    private var _opponentReleased = MutableStateFlow(false)
    val opponentReleased : StateFlow<Boolean> = _opponentReleased


    // ================================================================================ |
    // ============================ PUBLIC METHODS ==================================== |
    // ================================================================================ |

    // TODO: remove this
    override fun onCreate(intent: Intent) {
        // simulate opponent's movement
        viewModelScope.launch(Dispatchers.IO) {
            while(true){
                var angle = 0.0
                val (x, y) = angleToCoordinates(angle, SCREEN_CENTER.x.toDouble(), SCREEN_CENTER.y.toDouble(), OPPONENT_RADIUS_OUTER_EDGE.toDouble())
                _opponentTouchPoint.value = Pair(x, y)
                while(angle < 360.0 * 3.0){
                    // calculate coordinates
                    val (x, y) = angleToCoordinates(angle, SCREEN_CENTER.x.toDouble(), SCREEN_CENTER.y.toDouble(), OPPONENT_RADIUS_OUTER_EDGE.toDouble())
                    _opponentTouchPoint.value = Pair(x, y)
                    angle += 4.0
                    delay(16)
                }
                _opponentReleased.value = true
                delay(1000)
                _opponentReleased.value = false
            }
        }
    }

    fun setTouchPoint(x: Double, y: Double) {
        _touchPoint.value = Pair(x, y)
    }

    fun setReleased(bool: Boolean) {
        _released.value = bool
    }

    fun sendPosition(x: Double, y: Double, released: Boolean) {
        val position = Position(x, y, released)
//        viewModelScope.launch(Dispatchers.IO) {
//            model.sendPosition(position, getCurrentGameTime())
//        }
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

            }
            else -> super.handleGameAction(action)
        }
    }

    fun angleToCoordinates(angle: Double, centerX: Double, centerY: Double, radius: Double): Pair<Double, Double> {
        // Convert angle from degrees to radians
        val angleInRadians = toRadians(angle)

        // Calculate coordinates
        val x = centerX + radius * cos(angleInRadians)
        val y = centerY + radius * sin(angleInRadians)

        return Pair(x, y)
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

    companion object {
        private const val TAG = "WineGlassesViewModel"
    }
}