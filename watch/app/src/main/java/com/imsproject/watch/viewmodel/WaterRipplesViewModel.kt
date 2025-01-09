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
import com.imsproject.common.gameserver.GameAction
import com.imsproject.common.gameserver.GameType
import com.imsproject.common.gameserver.SessionEvent
import com.imsproject.watch.ACTIVITY_DEBUG_MODE
import com.imsproject.watch.BLUE_COLOR
import com.imsproject.watch.GRAY_COLOR
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
            val timestamp = super.getCurrentGameTime()
            model.sendUserInput(timestamp,packetTracker.newPacket())
            addEvent(SessionEvent.click(playerId,timestamp))
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
            GameAction.Type.USER_INPUT -> {
                val actor = action.actor ?: run{
                    Log.e(TAG, "handleGameAction: missing actor in user input action")
                    return
                }
                val timestamp = action.timestamp?.toLong() ?: run{
                    Log.e(TAG, "handleGameAction: missing timestamp in user input action")
                    return
                }
                val sequenceNumber = action.sequenceNumber ?: run{
                    Log.e(TAG, "handleGameAction: missing sequence number in user input action")
                    return
                }
                // switch to main thread to update UI
                withContext(Dispatchers.Main) {
                    showRipple(actor, timestamp)
                }
                if(actor == playerId){
                    packetTracker.receivedMyPacket(sequenceNumber)
                } else {
                    packetTracker.receivedOtherPacket(sequenceNumber)
                }
            }
            else -> super.handleGameAction(action)
        }
    }

    private fun showRipple(actor: String, timestamp : Long) {
        val rippleToCheck = if (actor == playerId) {
            ripples.find { it.actor != playerId }
        } else {
            ripples.find { it.actor == playerId }
        }

        // Synced click
        if (rippleToCheck != null && (rippleToCheck.timestamp - timestamp)
                                            .absoluteValue <= WATER_RIPPLES_SYNC_TIME_THRESHOLD) {
            rippleToCheck.color = VIVID_ORANGE_COLOR
            if (rippleToCheck.actor != playerId) {
                rippleToCheck.startingAlpha = 1.0f
                rippleToCheck.currentAlpha = (rippleToCheck.currentAlpha * 2).coerceAtMost(1.0f)
            }
            viewModelScope.launch(Dispatchers.IO) {
                addEvent(SessionEvent.syncedAtTime(playerId, timestamp))
            }
        }
        // not synced click
        else {
            val ripple = if (actor == playerId) {
                // My click
                Ripple(BLUE_COLOR, timestamp = timestamp, actor = actor)
            } else {
                // Other player's click
                Ripple(GRAY_COLOR, 0.5f, timestamp, actor)
            }
            ripples.add(0, ripple)
        }
        if (actor != playerId) {
            viewModelScope.launch(Dispatchers.IO) {
                delay(100)
                vibrator.vibrate(clickVibration)
            }
        }
        _counter.value++
    }

    companion object {
        private const val TAG = "WaterRipplesViewModel"
    }
}

