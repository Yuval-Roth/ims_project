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
import com.imsproject.watch.MY_SWEEP_ANGLE
import com.imsproject.watch.OPPONENT_STROKE_WIDTH
import com.imsproject.watch.OPPONENT_SWEEP_ANGLE

class WineGlassesViewModel() : GameViewModel(GameType.WINE_GLASSES) {

    class Arc private constructor(
        val color: Color,
        val size: Size,
        val sweepAngle: Float,
        val defaultAlpha: Float,
        val topLeft: Offset,
        val strokeWidth: Int
    ) {
        var currentAlpha = mutableFloatStateOf(defaultAlpha)
        var angleSkew = 0f
        var previousAngle = mutableFloatStateOf(UNDEFINED_ANGLE)
        var direction = 0f
        var startAngle = mutableFloatStateOf(0f)

        companion object {
            fun my() = Arc(
                GLOWING_YELLOW_COLOR,
                Size(MY_RADIUS_OUTER_EDGE * 2, MY_RADIUS_OUTER_EDGE * 2),
                MY_SWEEP_ANGLE,
                ARC_DEFAULT_ALPHA,
                Offset(SCREEN_CENTER.x - MY_RADIUS_OUTER_EDGE, SCREEN_CENTER.y - MY_RADIUS_OUTER_EDGE),
                MY_STROKE_WIDTH
            )
            fun opponent(): Arc {
                return Arc(
                    LIGHT_GRAY_COLOR,
                    Size(OPPONENT_RADIUS_OUTER_EDGE * 2, OPPONENT_RADIUS_OUTER_EDGE * 2),
                    OPPONENT_SWEEP_ANGLE,
                    ARC_DEFAULT_ALPHA,
                    Offset(SCREEN_CENTER.x - OPPONENT_RADIUS_OUTER_EDGE, SCREEN_CENTER.y - OPPONENT_RADIUS_OUTER_EDGE),
                    OPPONENT_STROKE_WIDTH
                )
            }
        }
    }

    // ================================================================================ |
    // ================================ STATE FIELDS ================================== |
    // ================================================================================ |

    //TODO: make this val
    lateinit var myArc : Arc
    lateinit var opponentArc : Arc

    private var _touchPoint = MutableStateFlow(Pair(-1.0,-1.0))
    val touchPoint : StateFlow<Pair<Double,Double>> = _touchPoint

    private var _released = MutableStateFlow(false)
    val released : StateFlow<Boolean> = _released

    private var _opponentReleased = MutableStateFlow(false)
    val opponentReleased : StateFlow<Boolean> = _opponentReleased


    // ================================================================================ |
    // ============================ PUBLIC METHODS ==================================== |
    // ================================================================================ |

    // TODO: remove this
    override fun onCreate(intent: Intent) {
        myArc = Arc.my()
        opponentArc = Arc.opponent()

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
        _touchPoint.value = Pair(x, y)
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