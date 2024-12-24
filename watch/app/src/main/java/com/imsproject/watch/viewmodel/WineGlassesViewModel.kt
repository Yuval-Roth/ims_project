package com.imsproject.watch.viewmodel

import android.content.Intent
import android.util.Log
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.imsproject.common.gameServer.GameAction
import com.imsproject.common.gameServer.GameRequest
import com.imsproject.common.gameServer.GameType
import com.imsproject.watch.GLOWING_YELLOW_COLOR
import com.imsproject.watch.MY_RADIUS_OUTER_EDGE
import com.imsproject.watch.MY_STROKE_WIDTH
import com.imsproject.watch.OPPONENT_RADIUS_OUTER_EDGE
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
import com.imsproject.watch.LIGHT_GRAY_COLOR
import com.imsproject.watch.MY_RADIUS_INNER_EDGE
import com.imsproject.watch.MY_SWEEP_ANGLE
import com.imsproject.watch.OPPONENT_STROKE_WIDTH
import com.imsproject.watch.OPPONENT_SWEEP_ANGLE
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

    // ================================================================================ |
    // ================================ STATE FIELDS ================================== |
    // ================================================================================ |

    //TODO: make this val
    lateinit var myArc : Arc
    lateinit var opponentArc : Arc

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

    // TODO: remove this
    override fun onCreate(intent: Intent) {
        myArc = Arc()
        opponentArc = Arc()

        // simulate opponent's movement
        viewModelScope.launch(Dispatchers.IO) {
            while(true){
                var counter = 0
                opponentArc.startAngle.floatValue = 0f
                while(counter < 360 * 3){
                    opponentArc.startAngle.floatValue = opponentArc.startAngle.floatValue + 4
                    counter += 4
                    delay(16)
                }
                _opponentReleased.value = true
                delay(1000)
                _opponentReleased.value = false
            }
        }
    }

    fun setTouchPoint(x: Double, y: Double) {
        val angle = calculateAngle(x, y)
        val distance = calculateDistance(x, y)
        _angle.value = angle
        _inBounds.value = MY_RADIUS_INNER_EDGE <= distance && distance <= MY_RADIUS_OUTER_EDGE
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

    fun setReleased(bool: Boolean) {
        _released.value = bool
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
                // switch to main thread to update UI
                withContext(Dispatchers.Main) {
                    //TODO: implement
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

    companion object {
        private const val TAG = "WineGlassesViewModel"
    }
}