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
import com.imsproject.watch.PACKAGE_PREFIX
import com.imsproject.watch.R
import com.imsproject.watch.SCREEN_RADIUS
import com.imsproject.watch.WAVE_MAX_ANIMATION_DURATION
import com.imsproject.watch.WAVE_MIN_ANIMATION_DURATION
import com.imsproject.watch.view.contracts.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.pow

class WavesViewModel: GameViewModel(GameType.WAVES) {

    class Wave(){
        var topLeft by mutableStateOf(Offset(-SCREEN_RADIUS * 0.7f, 0f))
        var animationProgress by mutableFloatStateOf(0f)
        var animationLength: Int = 0
        var direction by mutableIntStateOf(0)
    }

    private lateinit var soundPool: SoundPool
    private var strongWaveSoundId : Int = -1
    private var mediumWaveSoundId: Int = -1
    private var weakWaveSoundId: Int = -1


    // ================================================================================ |
    // ================================ STATE FIELDS ================================== |
    // ================================================================================ |

    val wave = Wave()

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
        weakWaveSoundId = soundPool.load(context,R.raw.wave_weak,1)


        if(ACTIVITY_DEBUG_MODE){
            viewModelScope.launch(Dispatchers.Default) {
                _turn.collect {
                    if(it == -myDirection){
                        delay(500)
                        val dpPerSec = (500..2500).random().toFloat()
                        withContext(Dispatchers.Main){
                            handleFling(dpPerSec, -myDirection)
                        }
                    }
                }
            }
            return
        }

        myDirection = intent.getStringExtra("$PACKAGE_PREFIX.additionalData").let {
            when (it) {
                "left" -> 1
                "right" -> -1
                else -> {
                    exitWithError("Missing or invalid direction data", Result.Code.BAD_REQUEST)
                    return
                }
            }
        }
        model.sessionSetupComplete()
    }

    fun handleFling(dpPerSec: Float, direction: Int){
        if(_turn.value == 0){
            Log.e(TAG, "fling: already animating" )
            return
        }

        oldTurn = _turn.value
        _turn.value = 0
        wave.animationLength = mapSpeedToDuration(dpPerSec)
        wave.direction = direction
        when (wave.animationLength){
            in 1500..2000 -> soundPool.play(strongWaveSoundId,1f,1f,0,0,1f)
            in 2001..3500 -> soundPool.play(mediumWaveSoundId,1f,1f,0,0,1f)
            in 3501..5000 -> soundPool.play(weakWaveSoundId,1f,1f,0,0,1f)
        }
    }

    fun fling(dpPerSec: Float){

        if(ACTIVITY_DEBUG_MODE){
            handleFling(dpPerSec, myDirection)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val timestamp = super.getCurrentGameTime()
            val data = "${dpPerSec},$myDirection"
            model.sendUserInput(timestamp,packetTracker.newPacket(), data)
            addEvent(SessionEvent.fling(playerId,timestamp, data))
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
                val (dpPerSec, direction) = data.split(",")
                handleFling(dpPerSec.toFloat(), direction.toInt())

                if(actor == playerId){
                    packetTracker.receivedMyPacket(sequenceNumber)
                } else {
                    packetTracker.receivedOtherPacket(sequenceNumber)
                    addEvent(SessionEvent.opponentFling(playerId, arrivedTimestamp,data))
                }
            }
            else -> super.handleGameAction(action)
        }
    }

    private fun mapSpeedToDuration(pxPerSec: Float): Int {
        val duration = if (pxPerSec <= 1000f) {
            WAVE_MAX_ANIMATION_DURATION
        } else {
            val v = 1000f / pxPerSec
            val duration = WAVE_MAX_ANIMATION_DURATION * v.pow(1.5f)
            duration.toInt().coerceIn(WAVE_MIN_ANIMATION_DURATION, WAVE_MAX_ANIMATION_DURATION)
        }
        return duration
    }

    companion object {
        private const val TAG = "WavesViewModel"
    }

}