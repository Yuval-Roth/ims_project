package com.imsproject.watch.viewmodel

import android.content.Context
import android.content.Intent
import android.os.VibrationEffect
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.imsproject.watch.view.contracts.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.math.absoluteValue


class FlowerGardenViewModel() : GameViewModel(GameType.FLOWER_GARDEN) {



    private lateinit var clickVibration : VibrationEffect

    // ================================================================================ |
    // ================================ STATE FIELDS ================================== |
    // ================================================================================ |


    private var _counter = MutableStateFlow(0)
    val counter: StateFlow<Int> = _counter

    // ================================================================================ |
    // ============================ PUBLIC METHODS ==================================== |
    // ================================================================================ |

    fun click() {
        if(ACTIVITY_DEBUG_MODE){
//            showRipple(playerId, System.currentTimeMillis())
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
//                    showRipple("player1", System.currentTimeMillis())
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
//                showRipple(actor, timestamp)
                
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


    companion object {
        private const val TAG = "WaterRipplesViewModel"
    }
}

