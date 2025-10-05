package com.imsproject.watch.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.imsproject.common.gameserver.GameAction
import com.imsproject.common.gameserver.GameType
import com.imsproject.common.gameserver.SessionEvent
import com.imsproject.watch.ACTIVITY_DEBUG_MODE
import com.imsproject.watch.SCREEN_RADIUS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.pow

class WavesViewModel: GameViewModel(GameType.WAVES) {

    enum class AnimationStage {
        NOT_STARTED,
        FIRST,
        SECOND,
        DONE
    }

    class Wave(
        val color: Color,
        val animationLength: Int,
        val direction: Int
    ){
        var topLeft by mutableStateOf(Offset(-SCREEN_RADIUS, SCREEN_RADIUS))
        var size by mutableStateOf(Size(SCREEN_RADIUS * 2f, SCREEN_RADIUS * 3f))
        var animationStage by mutableStateOf(AnimationStage.NOT_STARTED)
        var animationProgress by mutableFloatStateOf(0f)
    }

//    if (i % 2 == 0) Color(0xFF3B82F6).copy(alpha = 0.8f)
//    else Color(0xFFFFD8D8).copy(alpha = 0.8f)


    // ================================================================================ |
    // ================================ STATE FIELDS ================================== |
    // ================================================================================ |

    val waves = mutableStateListOf<Wave>()

    var myDirection: Int = 1
        private set

    private val _turn = MutableStateFlow(1)
    val turn: StateFlow<Int> = _turn
    private var oldTurn = 1


    // ================================================================================ |
    // ============================ PUBLIC METHODS ==================================== |
    // ================================================================================ |


    override fun onCreate(intent: Intent, context: Context) {
        super.onCreate(intent, context)

        if(ACTIVITY_DEBUG_MODE){
//            viewModelScope.launch(Dispatchers.Default) {
//                val direction = -1
//                _turn.collect {
//                    if(it == direction){
//                        delay(500)
//                        val dpPerSec = (500..2500).random().toFloat()
//                        withContext(Dispatchers.Main){
//                            fling(dpPerSec, direction)
//                        }
//                    }
//                }
//            }
            return
        }
    }

    fun fling(dpPerSec: Float, direction: Int){
        if(_turn.value == 0){
            Log.e(TAG, "fling: already animating" )
            return
        }

        require(direction != 0) { "direction must be positive or negative" }

        oldTurn = _turn.value
        _turn.value = 0
        val animationLength = mapSpeedToDuration(dpPerSec, 1500, 5000)
        val wave = Wave(
            color = if (direction > 0) Color(0xFF3B82F6)
                    else Color(0xFFFFD8D8),
            animationLength = animationLength,
            direction = direction
        )
        waves.add(wave)
    }

    fun flipTurn(){
        _turn.value = -oldTurn

        // clean up old waves
        if(waves.size >= 3){
            waves.removeAt(0)
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
                val data = action.data?: run{
                    Log.e(TAG, "handleGameAction: missing data in user input action")
                    return
                }
                val sequenceNumber = action.sequenceNumber ?: run{
                    Log.e(TAG, "handleGameAction: missing sequence number in user input action")
                    return
                }

                val arrivedTimestamp = getCurrentGameTime()
                // TODO: handle data received

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

    private fun mapSpeedToDuration(pxPerSec: Float, minDurationMs: Int, maxDurationMs: Int): Int {
        val duration = if (pxPerSec <= 1000f) {
            maxDurationMs
        } else {
            val v = 1000f / pxPerSec
            val duration = maxDurationMs * v.pow(1.5f)
            duration.toInt().coerceIn(minDurationMs, maxDurationMs)
        }
        return duration
    }

    companion object {
        private const val TAG = "WavesViewModel"
    }

}