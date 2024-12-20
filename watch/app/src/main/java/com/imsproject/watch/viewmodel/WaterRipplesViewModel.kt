package com.imsproject.watch.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.imsproject.common.gameServer.GameAction
import com.imsproject.common.gameServer.GameRequest
import com.imsproject.common.gameServer.GameType
import com.imsproject.watch.GRAY_COLOR
import com.imsproject.watch.LIGHT_BLUE_COLOR
import com.imsproject.watch.VIVID_ORANGE_COLOR
import com.imsproject.watch.WATER_RIPPLES_BUTTON_SIZE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue

private const val SYNC_TIME_THRESHOLD = 75*1000000

class WaterRipplesViewModel() : GameViewModel(GameType.WATER_RIPPLES) {

    class Ripple(
        color: Color,
        var startingAlpha: Float = 1f,
        val timestamp: Long,
        val actor: String
    ) {
        var color = mutableStateOf(color)
        var size = mutableFloatStateOf(WATER_RIPPLES_BUTTON_SIZE.toFloat())
        var currentAlpha = mutableFloatStateOf(startingAlpha)
    }

    // ================================================================================ |
    // ================================ STATE FIELDS ================================== |
    // ================================================================================ |

    val ripples = mutableStateListOf<Ripple>()

    private var _counter = MutableStateFlow(0)
    val counter : StateFlow<Int> = _counter

    // ================================================================================ |
    // ============================ PUBLIC METHODS ==================================== |
    // ================================================================================ |

    fun click() {
        viewModelScope.launch(Dispatchers.IO) {
            model.sendClick(super.getCurrentGameTime())
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
            GameAction.Type.CLICK -> {
                val actor = action.actor ?: run{
                    Log.e(TAG, "handleGameAction: missing actor in click action")
                    return
                }
                val timestamp = action.timestamp?.toLong() ?: run{
                    Log.e(TAG, "handleGameAction: missing timestamp in click action")
                    return
                }
                // switch to main thread to update UI
                withContext(Dispatchers.Main) {
                    showRipple(actor, timestamp)
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

    private fun showRipple(actor: String, timestamp : Long) {
        println("$actor: $timestamp")
        val rippleToCheck = if (actor == playerId){
            ripples.find { it.actor != playerId }
        } else {
            ripples.find { it.actor == playerId }
        }

        if(rippleToCheck != null
            && (rippleToCheck.timestamp - timestamp).absoluteValue <= SYNC_TIME_THRESHOLD
        ){
            rippleToCheck.color.value = VIVID_ORANGE_COLOR
            if(rippleToCheck.actor != playerId){
                rippleToCheck.startingAlpha = 1.0f
                rippleToCheck.currentAlpha.floatValue =
                    (rippleToCheck.currentAlpha.floatValue * 2).coerceAtMost(1.0f)
            }
        } else {
            val ripple = if(actor == playerId){
                // My click
                Ripple(LIGHT_BLUE_COLOR, timestamp = timestamp, actor = actor)
            } else  {
                // Other player's click
                Ripple(GRAY_COLOR,0.5f,timestamp,actor)
            }
            ripples.add(0,ripple)
        }
        _counter.value++
    }

    companion object {
        private const val TAG = "WaterRipplesViewModel"
    }
}

