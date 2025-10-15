package com.imsproject.watch.viewmodel

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.viewModelScope
import com.imsproject.common.gameserver.GameAction
import com.imsproject.common.gameserver.GameType
import com.imsproject.common.gameserver.SessionEvent
import com.imsproject.watch.ACTIVITY_DEBUG_MODE
import com.imsproject.watch.R
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

    class Wave(){
        var topLeft by mutableStateOf(Offset(-SCREEN_RADIUS * 0.7f, 0f))
        var animationStage by mutableStateOf(AnimationStage.NOT_STARTED)
        var animationProgress by mutableFloatStateOf(0f)
        var animationLength: Int = 0
        var direction by mutableIntStateOf(0)
    }

//    if (i % 2 == 0) Color(0xFF3B82F6).copy(alpha = 0.8f)
//    else Color(0xFFFFD8D8).copy(alpha = 0.8f)

    private lateinit var soundPool: SoundPool
    private var strongWaveSoundId : Int = -1
    private var mediumWaveSoundId: Int = -1


    // ================================================================================ |
    // ================================ STATE FIELDS ================================== |
    // ================================================================================ |

    val wave by lazy { Wave() }

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

        soundPool = SoundPool.Builder().setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build()).setMaxStreams(1).build()
        strongWaveSoundId = soundPool.load(context, R.raw.wave_strong, 1)
        mediumWaveSoundId = soundPool.load(context,R.raw.wave_medium,1)


        if(ACTIVITY_DEBUG_MODE){
            viewModelScope.launch(Dispatchers.Default) {
                val direction = -1
                _turn.collect {
                    if(it == direction){
                        delay(500)
                        val dpPerSec = (500..2500).random().toFloat()
                        withContext(Dispatchers.Main){
                            fling(dpPerSec, direction)
                        }
                    }
                }
            }
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
        wave.animationLength = mapSpeedToDuration(dpPerSec, 1500, 5000)
        wave.direction = direction
        when (wave.animationLength){
            in 1500..2500 -> soundPool.play(strongWaveSoundId,1f,1f,0,0,1f)
            in 2501..5000 -> soundPool.play(mediumWaveSoundId,1f,1f,0,0,1f)
        }
    }

    fun flipTurn(){
        _turn.value = -oldTurn
        wave.direction = 0
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