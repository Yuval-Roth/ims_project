package com.imsproject.watch.viewmodel

import android.content.Intent
import android.util.Log
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.imsproject.common.gameServer.GameAction
import com.imsproject.common.gameServer.GameRequest
import com.imsproject.common.gameServer.GameType
import com.imsproject.watch.GLOWING_YELLOW_COLOR
import com.imsproject.watch.MY_RADIUS_OUTER_EDGE
import com.imsproject.watch.OPPONENT_RADIUS_OUTER_EDGE
import com.imsproject.watch.UNDEFINED_ANGLE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class WineGlassesViewModel() : GameViewModel(GameType.WINE_GLASSES) {


//    var touchPoint = remember { mutableStateOf<Pair<Double,Double>>(Pair(-1.0,-1.0)) }
//
//    // ============= touch point related values =========== |
//    val center = remember {Offset(SCREEN_WIDTH / 2f, SCREEN_WIDTH / 2f)}
//    val radiusOuterEdge = remember{ (SCREEN_WIDTH / 2).toFloat() }
//    val radiusInnerEdge = remember { (SCREEN_WIDTH / 2) * 0.2f }
//    val released = remember { mutableStateOf(false) }
//    // ===================================================== |
//
//    // ============= arc related values ================= |
//    val defaultAlpha = remember { 0.8f }
//    val alpha = remember { mutableFloatStateOf(0.8f) }
//    val arcSize = remember { Size(radiusOuterEdge * 2, radiusOuterEdge * 2) }
//    val sweepAngle = remember { 30f }
//    var angleSkew = remember { 0f }
//    var previousAngle = remember { UNDEFINED_ANGLE }
//    var direction = remember { 0f }
//    var arcStartAngle = remember { mutableFloatStateOf(UNDEFINED_ANGLE) }
//    // ================================================== |

    class Arc(
        val color: Color,
        val size: Size,
        val sweepAngle: Float,
        val defaultAlpha: Float,
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
                30f, // sweep angle
                0.8f // default alpha
            )
            fun opponent() = Arc(
                Color(0xFFD5D5D5),
                Size(OPPONENT_RADIUS_OUTER_EDGE * 2, OPPONENT_RADIUS_OUTER_EDGE * 2),
                30f,
                0.8f
            )
        }
    }

    // ================================================================================ |
    // ================================ STATE FIELDS ================================== |
    // ================================================================================ |

    //TODO: make this val
    lateinit var myArc : Arc
    lateinit var opponentArc : Arc

    private var _released = MutableStateFlow(false)
    val released : StateFlow<Boolean> = _released

    private var _touchPoint = MutableStateFlow(Pair(-1.0,-1.0))
    val touchPoint : StateFlow<Pair<Double,Double>> = _touchPoint

    private var _opponentAngle = MutableStateFlow(UNDEFINED_ANGLE)
    val opponentAngle : StateFlow<Float> = _opponentAngle

    private var _inBounds = MutableStateFlow(false)
    val inBounds : StateFlow<Boolean> = _inBounds



    // ================================================================================ |
    // ============================ PUBLIC METHODS ==================================== |
    // ================================================================================ |

    // TODO: remove this
    override fun onCreate(intent: Intent) {
        myArc = Arc.my()
        opponentArc = Arc.opponent()
    }

    fun setTouchPoint(x: Double, y: Double) {
        _touchPoint.value = Pair(x, y)
    }

    fun setReleased(bool: Boolean) {
        _released.value = bool
    }

    fun setInBounds(bool: Boolean) {
        _inBounds.value = bool
    }

    fun shouldShowArc() = _inBounds.value && !_released.value

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