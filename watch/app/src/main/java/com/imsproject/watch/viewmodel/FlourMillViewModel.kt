package com.imsproject.watch.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.fastCoerceIn
import androidx.lifecycle.viewModelScope
import com.imsproject.common.gameserver.GameAction
import com.imsproject.common.gameserver.GameType
import com.imsproject.common.gameserver.SessionEvent
import com.imsproject.watch.ACTIVITY_DEBUG_MODE
import com.imsproject.watch.PACKAGE_PREFIX
import com.imsproject.watch.SCREEN_RADIUS
import com.imsproject.common.utils.Angle
import com.imsproject.common.utils.UNDEFINED_ANGLE
import com.imsproject.watch.ARC_DEFAULT_ALPHA
import com.imsproject.watch.FLOUR_MILL_SYNC_FREQUENCY_THRESHOLD
import com.imsproject.watch.FREQUENCY_HISTORY_MILLISECONDS
import com.imsproject.watch.INNER_TOUCH_POINT
import com.imsproject.watch.MAX_ANGLE_SKEW
import com.imsproject.watch.MIN_ANGLE_SKEW
import com.imsproject.watch.MY_SWEEP_ANGLE
import com.imsproject.watch.OUTER_TOUCH_POINT
import com.imsproject.watch.FLOUR_MILL_SYNC_FREQUENCY_THRESHOLD
import com.imsproject.watch.utils.Arc
import com.imsproject.watch.utils.FrequencyTracker
import com.imsproject.watch.utils.PacketTracker
import com.imsproject.watch.utils.WavPlayer
import com.imsproject.watch.utils.cartesianToPolar
import com.imsproject.watch.utils.isBetweenInclusive
import com.imsproject.watch.utils.toAngle
import com.imsproject.watch.view.contracts.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.absoluteValue

class FlourMillViewModel : GameViewModel(GameType.FLOUR_MILL) {

    // ================================================================================ |
    // ================================ STATE FIELDS ================================== |
    // ================================================================================ |

    val myArc = Arc()
    val opponentArc = Arc()
    private lateinit var myFrequencyTracker : FrequencyTracker
    @Volatile
    private var opponentFrequency = 0f

    private var _released = MutableStateFlow(true)
    val released : StateFlow<Boolean> = _released

    private var _opponentReleased = MutableStateFlow(false)
    val opponentReleased : StateFlow<Boolean> = _opponentReleased

    private var inSync = false
        set(value) {
            if(field != value){
                val timestamp = getCurrentGameTime()
                viewModelScope.launch(Dispatchers.Default) {
                    when (value) {
                        true -> addEvent(SessionEvent.syncStartTime(playerId, timestamp))
                        false -> addEvent(SessionEvent.syncEndTime(playerId, timestamp))
                    }
                }
                field = value
            }
        }

    // ================================================================================ |
    // ============================ PUBLIC METHODS ==================================== |
    // ================================================================================ |

    override fun onCreate(intent: Intent, context: Context) {
        super.onCreate(intent,context)
        
        myFrequencyTracker = FrequencyTracker()

        if(ACTIVITY_DEBUG_MODE){
            viewModelScope.launch(Dispatchers.Default) {
                val opponentFrequencyTracker = FrequencyTracker()
                while(true) {
                    var rawAngle = 0.0f
                    while(rawAngle < 360 * 15){
                        var angle = rawAngle % 360
                        if(angle > 180) angle -= 360
                        opponentFrequencyTracker.addSample(angle.toAngle())
                        opponentFrequency = opponentFrequencyTracker.frequency
                        opponentArc.updateArc(angle.toAngle())
                        rawAngle += 4
                        delay(16)
                    }
                    _opponentReleased.value = true
                    opponentFrequency = 0f
                    opponentFrequencyTracker.reset()
                    delay(2000)
                    _opponentReleased.value = false
                }
            }
            return
        }

        // set up sync params
        val syncTolerance = intent.getLongExtra("$PACKAGE_PREFIX.syncTolerance", -1)
        if (syncTolerance <= 0L) {
            exitWithError("Missing sync tolerance", Result.Code.BAD_REQUEST)
            return
        }
        val syncWindowLength = intent.getLongExtra("$PACKAGE_PREFIX.syncWindowLength", -1)
        if (syncWindowLength <= 0L) {
            exitWithError("Missing sync window length", Result.Code.BAD_REQUEST)
            return
        }
        FLOUR_MILL_SYNC_FREQUENCY_THRESHOLD = syncTolerance.toFloat() * 0.01f
        FREQUENCY_HISTORY_MILLISECONDS = syncWindowLength
        Log.d(TAG, "syncTolerance: $syncTolerance")
        Log.d(TAG, "syncWindowLength: $syncWindowLength")

        // start the frequency tracking loop
        viewModelScope.launch(Dispatchers.Default){
            while(true){
                delay(100) // run this loop roughly 10 times per second
                val timestamp = getCurrentGameTime()
                addEvent(SessionEvent.frequency(playerId, timestamp, myFrequencyTracker.frequency.toString()))
                addEvent(SessionEvent.opponentFrequency(playerId,timestamp,opponentFrequency.toString()))
            }
        }
    }

    fun setTouchPoint(x: Float, y: Float) {
        val (distance,rawAngle) = cartesianToPolar(x, y)
        val inBounds = if(x != -1.0f && y != -1.0f){
            distance in INNER_TOUCH_POINT..OUTER_TOUCH_POINT
        } else {
            false // not touching the screen
        }

        if(inBounds){
            myArc.updateArc(rawAngle)
            _released.value = false
            myFrequencyTracker.addSample(rawAngle)
        } else {
            _released.value = true
            myFrequencyTracker.reset()
        }

        if(ACTIVITY_DEBUG_MODE) return

        // send input to server
        viewModelScope.launch(Dispatchers.IO) {
            val timestamp = getCurrentGameTime()
            val angle = if(inBounds) rawAngle else UNDEFINED_ANGLE
            val frequency = myFrequencyTracker.frequency
            val data = "$angle,$frequency"
            model.sendUserInput(timestamp, packetTracker.newPacket(),data)
            addEvent(SessionEvent.angle(playerId,timestamp,data))
        }
    }

    fun inSync() = (
            !released.value && !opponentReleased.value
                    && (myFrequencyTracker.frequency - opponentFrequency)
                .absoluteValue < FLOUR_MILL_SYNC_FREQUENCY_THRESHOLD
            ).also { inSync = it }

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

                val outOfOrder = packetTracker.receivedOtherPacket(sequenceNumber)
                if(outOfOrder) return

                val (rawAngle, frequency) = data.split(",").let{
                    Pair(it[0].toFloat(), it[1].toFloat())
                }
                if(rawAngle == UNDEFINED_ANGLE){
                    _opponentReleased.value = true
                    opponentFrequency = 0f
                } else {
                    _opponentReleased.value = false
                    opponentFrequency = frequency
                    opponentArc.updateArc(rawAngle.toAngle())
                }

                addEvent(SessionEvent.opponentAngle(playerId,arrivedTimestamp,rawAngle.toString()))
            }
            else -> super.handleGameAction(action)
        }
    }

    companion object {
        private const val TAG = "FlourMillViewModel"
    }
}