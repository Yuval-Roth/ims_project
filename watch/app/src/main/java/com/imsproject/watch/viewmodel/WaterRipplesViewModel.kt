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
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.lifecycle.viewModelScope
import com.imsproject.common.gameserver.GameAction
import com.imsproject.common.gameserver.GameType
import com.imsproject.common.gameserver.SessionEvent
import com.imsproject.watch.ACTIVITY_DEBUG_MODE
import com.imsproject.watch.BLUE_COLOR
import com.imsproject.watch.GRAY_COLOR
import com.imsproject.watch.PACKAGE_PREFIX
import com.imsproject.watch.RIPPLE_MAX_SIZE
import com.imsproject.watch.VIVID_ORANGE_COLOR
import com.imsproject.watch.WATER_RIPPLES_ANIMATION_DURATION
import com.imsproject.watch.WATER_RIPPLES_BUTTON_SIZE
import com.imsproject.watch.WATER_RIPPLES_SYNC_TIME_THRESHOLD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.math.absoluteValue
import com.imsproject.watch.view.contracts.Result


class WaterRipplesViewModel() : GameViewModel(GameType.WATER_RIPPLES) {

    class Ripple(
        var color: Color,
        startingAlpha: Float = 1f,
        val timestamp: Long,
        val actor: String
    ) {
        //TODO: ADJUST THE STARTING SIZE EVERYWHERE
        var size by mutableFloatStateOf(WATER_RIPPLES_BUTTON_SIZE.toFloat())
        var currentAlpha by mutableFloatStateOf(startingAlpha)
        val sizeStep = (RIPPLE_MAX_SIZE - WATER_RIPPLES_BUTTON_SIZE) / (WATER_RIPPLES_ANIMATION_DURATION / 16f)
        var alphaStep = startingAlpha / (WATER_RIPPLES_ANIMATION_DURATION / 16f)

        fun updateAlphaStep(){
            alphaStep =  currentAlpha / (WATER_RIPPLES_ANIMATION_DURATION / 16f)
        }
    }

    private lateinit var clickVibration : VibrationEffect

    // ================================================================================ |
    // ================================ STATE FIELDS ================================== |
    // ================================================================================ |

    val ripples = ConcurrentLinkedDeque<Ripple>()

    private var _counter = MutableStateFlow(0)
    val counter: StateFlow<Int> = _counter

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
            viewModelScope.launch(Dispatchers.Default) {
                while(true){
                    delay(1000)
                    showRipple("player1", System.currentTimeMillis())
                }
            }
            return
        }

        val syncTolerance = intent.getLongExtra("$PACKAGE_PREFIX.syncTolerance", -1)
        if (syncTolerance <= 0L) {
            exitWithError("Missing sync tolerance", Result.Code.BAD_REQUEST)
            return
        }
        WATER_RIPPLES_SYNC_TIME_THRESHOLD = syncTolerance.toInt()
        Log.d(TAG, "syncTolerance: $syncTolerance")
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

                val arrivedTimestamp = getCurrentGameTime()
                showRipple(actor, timestamp)
                
                if(actor == playerId){
                    packetTracker.receivedMyPacket(sequenceNumber)
                } else {
                    packetTracker.receivedOtherPacket(sequenceNumber)
                    addEvent(SessionEvent.opponentClick(playerId, arrivedTimestamp))
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
                rippleToCheck.currentAlpha = (rippleToCheck.currentAlpha * 2).fastCoerceAtMost(1.0f)
                rippleToCheck.updateAlphaStep()
            }
            viewModelScope.launch(Dispatchers.Default) {
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
            ripples.addFirst(ripple)
        }
        if (actor != playerId) {
            viewModelScope.launch(Dispatchers.IO) {
                delay(100)
                vibrator.vibrate(clickVibration)
            }
        }
        _counter.value++ // used to trigger recomposition
    }

    companion object {
        private const val TAG = "WaterRipplesViewModel"
    }
}

