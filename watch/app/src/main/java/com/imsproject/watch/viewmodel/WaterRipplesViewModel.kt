package com.imsproject.watch.viewmodel

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.SoundPool
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
import com.imsproject.watch.GRASS_GREEN_COLOR
import com.imsproject.watch.GRAY_COLOR
import com.imsproject.watch.PACKAGE_PREFIX
import com.imsproject.watch.R
import com.imsproject.watch.RIPPLE_MAX_SIZE
import com.imsproject.watch.WATER_RIPPLES_ANIMATION_DURATION
import com.imsproject.watch.WATER_RIPPLES_BUTTON_SIZE
import com.imsproject.watch.WATER_RIPPLES_SYNC_TIME_THRESHOLD
import com.imsproject.watch.view.contracts.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.math.absoluteValue


open class WaterRipplesViewModel() : GameViewModel(GameType.WATER_RIPPLES) {

    class Ripple(
        var color: Color,
        val timestamp: Long,
        val actor: String,
        startingAlpha: Float = 1f
    ) {
        var size by mutableFloatStateOf(WATER_RIPPLES_BUTTON_SIZE.toFloat())
        var currentAlpha by mutableFloatStateOf(startingAlpha)
        val sizeStep = (RIPPLE_MAX_SIZE - WATER_RIPPLES_BUTTON_SIZE) / (WATER_RIPPLES_ANIMATION_DURATION / 16f)
        var alphaStep = startingAlpha / (WATER_RIPPLES_ANIMATION_DURATION / 16f)
    }

    private lateinit var clickVibration : VibrationEffect
    private lateinit var soundPool: SoundPool
    private var waterDropSoundId : Int = -1

    // ================================================================================ |
    // ================================ STATE FIELDS ================================== |
    // ================================================================================ |

    val ripples = ConcurrentLinkedDeque<Ripple>()

    protected var _counter = MutableStateFlow(0)

    var myColor: Color = BLUE_COLOR
    var opponentColor: Color = GRASS_GREEN_COLOR

    val counter: StateFlow<Int> = _counter

    // ================================================================================ |
    // ============================ PUBLIC METHODS ==================================== |
    // ================================================================================ |

    open fun click() {
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

        clickVibration = VibrationEffect.createWaveform(
            longArrayOf(0, 20,10,10,10),
            intArrayOf(0, 200,120,100,80),
            -1
        )
        soundPool = SoundPool.Builder().setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build()).setMaxStreams(1).build()
        waterDropSoundId = soundPool.load(context, R.raw.water_drop, 1)

        if(ACTIVITY_DEBUG_MODE){
            viewModelScope.launch(Dispatchers.Default) {
                while(true){
                    delay(1000)
                    showRipple("player1", System.currentTimeMillis())
                }
            }
            return
        }
        val color = intent.getStringExtra("$PACKAGE_PREFIX.additionalData")
        when(color) {
            "blue" -> {
                myColor = BLUE_COLOR
                opponentColor = GRASS_GREEN_COLOR
            }
            "green" -> {
                myColor = GRASS_GREEN_COLOR
                opponentColor = BLUE_COLOR
            }
            else -> {
                exitWithError("Invalid color data", Result.Code.BAD_REQUEST)
                return
            }
        }
        val syncTolerance = intent.getLongExtra("$PACKAGE_PREFIX.syncTolerance", -1)
        if (syncTolerance <= 0L) {
            exitWithError("Missing sync tolerance", Result.Code.BAD_REQUEST)
            return
        }
        WATER_RIPPLES_SYNC_TIME_THRESHOLD = syncTolerance.toInt()
        Log.d(TAG, "syncTolerance: $syncTolerance")
        Log.d(TAG, "My color: $myColor")
        viewModelScope.launch(Dispatchers.IO) {
            model.sessionSetupComplete()
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

        // find the latest ripple that is not by the same actor
        val rippleToCheck = if (actor == playerId) {
            ripples.find { it.actor != playerId }
        } else {
            ripples.find { it.actor == playerId }
        }

        // Synced click
        if (rippleToCheck != null && (rippleToCheck.timestamp - timestamp)
                                            .absoluteValue <= WATER_RIPPLES_SYNC_TIME_THRESHOLD) {
//            rippleToCheck.color = Color(0xFFF9C429)
//            if (rippleToCheck.actor != playerId) {
//                // update the ripple's alpha to make it seem like it started from 1.0f and not from 0.5f
//                val newAlpha = (rippleToCheck.currentAlpha * 2).fastCoerceAtMost(1.0f)
//                rippleToCheck.currentAlpha = newAlpha
//                rippleToCheck.alphaStep = newAlpha / (WATER_RIPPLES_ANIMATION_DURATION / 16f)
//            }
            viewModelScope.launch(Dispatchers.IO) {
                soundPool.play(waterDropSoundId, 1f, 1f, 0, 0, 1f)
                delay(100)
                vibrator.vibrate(clickVibration)
            }
            addEvent(SessionEvent.syncedAtTime(playerId, timestamp))
        }
//        // not synced click
//        else {
            val ripple = if (actor == playerId) {
                // My click
                Ripple(myColor,timestamp,actor,0.35f)
            } else {
                // Other player's click
                Ripple(opponentColor, timestamp, actor, 0.35f)
            }
            ripples.addFirst(ripple)
//        }
        _counter.value++ // used to trigger recomposition
    }

    companion object {
        private const val TAG = "WaterRipplesViewModel"
    }
}

