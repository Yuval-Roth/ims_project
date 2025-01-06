package com.imsproject.watch.viewmodel

import android.content.Context
import android.content.Intent
import android.os.VibrationEffect
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.imsproject.common.gameServer.GameAction
import com.imsproject.common.gameServer.GameRequest
import com.imsproject.common.gameServer.GameType
import com.imsproject.watch.ACTIVITY_DEBUG_MODE
import com.imsproject.watch.BLUE_COLOR
import com.imsproject.watch.GRAY_COLOR
import com.imsproject.watch.LIGHT_BLUE_COLOR
import com.imsproject.watch.VIVID_ORANGE_COLOR
import com.imsproject.watch.WATER_RIPPLES_BUTTON_SIZE
import com.imsproject.watch.WATER_RIPPLES_SYNC_TIME_THRESHOLD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue


class WaterRipplesViewModel() : GameViewModel(GameType.WATER_RIPPLES) {

    class Ripple(
        color: Color,
        var startingAlpha: Float = 1f,
        val timestamp: Long,
        val actor: String
    ) {
        var color by mutableStateOf(color)
        var size by mutableFloatStateOf(WATER_RIPPLES_BUTTON_SIZE.toFloat())
        var currentAlpha by mutableFloatStateOf(startingAlpha)
    }

    private lateinit var clickVibration : VibrationEffect

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
        if(ACTIVITY_DEBUG_MODE){
            showRipple(playerId, System.currentTimeMillis())
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            model.sendClick(super.getCurrentGameTime())
        }
    }

    override fun onCreate(intent: Intent, context: Context) {
        super.onCreate(intent, context)

        clickVibration = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)

        if(ACTIVITY_DEBUG_MODE){
            viewModelScope.launch(Dispatchers.IO) {
                while(true){
                    delay(1000)
                    withContext(Dispatchers.Main){
                        showRipple("player1", System.currentTimeMillis())
                    }
                }
            }
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
        val rippleToCheck = if (actor == playerId){
            ripples.find { it.actor != playerId }
        } else {
            ripples.find { it.actor == playerId }
        }

        if(rippleToCheck != null
            && (rippleToCheck.timestamp - timestamp).absoluteValue <= WATER_RIPPLES_SYNC_TIME_THRESHOLD
        ){
            rippleToCheck.color = VIVID_ORANGE_COLOR
            if(rippleToCheck.actor != playerId){
                rippleToCheck.startingAlpha = 1.0f
                rippleToCheck.currentAlpha = (rippleToCheck.currentAlpha * 2).coerceAtMost(1.0f)
            }
        } else {
            val ripple = if(actor == playerId){
                // My click
                Ripple(BLUE_COLOR, timestamp = timestamp, actor = actor)
            } else  {
                // Other player's click
                Ripple(GRAY_COLOR,0.5f,timestamp,actor)
            }
            ripples.add(0,ripple)
        }
        viewModelScope.launch(Dispatchers.IO) {
            delay(100)
            if(actor != playerId){
                vibrator.vibrate(clickVibration)
            }
        }

        _counter.value++
    }

    companion object {
        private const val TAG = "WaterRipplesViewModel"
    }
}

