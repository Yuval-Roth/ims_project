package com.imsproject.watch.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.imsproject.common.gameserver.GameAction
import com.imsproject.common.gameserver.GameType
import com.imsproject.common.gameserver.SessionEvent
import com.imsproject.watch.utils.Angle
import com.imsproject.watch.utils.UNDEFINED_ANGLE
import com.imsproject.watch.ACTIVITY_DEBUG_MODE
import com.imsproject.watch.BLUE_COLOR
import com.imsproject.watch.FLOUR_MILL_SYNC_FREQUENCY_THRESHOLD
import com.imsproject.watch.FREQUENCY_HISTORY_MILLISECONDS
import com.imsproject.watch.GRASS_GREEN_COLOR
import com.imsproject.watch.INNER_TOUCH_POINT
import com.imsproject.watch.MILL_SOUND_TRACK
import com.imsproject.watch.OUTER_TOUCH_POINT
import com.imsproject.watch.PACKAGE_PREFIX
import com.imsproject.watch.R
import com.imsproject.watch.utils.Arc
import com.imsproject.watch.utils.FrequencyTracker
import com.imsproject.watch.utils.cartesianToPolar
import com.imsproject.watch.utils.toAngle
import com.imsproject.watch.view.contracts.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private const val RADIANS_IN_CIRCLE = 6.283f

open class FlourMillViewModel : GameViewModel(GameType.FLOUR_MILL) {

    // ================================================================================ |
    // ================================ STATE FIELDS ================================== |
    // ================================================================================ |

    var myColor: Color = BLUE_COLOR
    var opponentColor: Color = GRASS_GREEN_COLOR

    val myArc by lazy { Arc(myColor) }
    val opponentArc by lazy { Arc(opponentColor) }
    protected lateinit var myFrequencyTracker : FrequencyTracker
    @Volatile
    private var opponentFrequency = 0f

    protected var _released = MutableStateFlow(true)
    val released : StateFlow<Boolean> = _released

    private var _opponentReleased = MutableStateFlow(false)
    val opponentReleased : StateFlow<Boolean> = _opponentReleased

    private val _targetWheelAngle = MutableStateFlow(Angle(0f))
    val targetWheelAngle: StateFlow<Angle> = _targetWheelAngle

    @Volatile
    var inSync = false
        private set

    // ================================================================================ |
    // ============================ PUBLIC METHODS ==================================== |
    // ================================================================================ |

    override fun onCreate(intent: Intent, context: Context) {
        super.onCreate(intent,context)

        setupWavPlayer()
        myFrequencyTracker = FrequencyTracker()

        // sync checking loop
        viewModelScope.launch(Dispatchers.Default){
            while(true){
                delay(16)
                val timestamp = getCurrentGameTime()
                val myFrequency = myFrequencyTracker.frequency
                val opponentFrequency = opponentFrequency
                if(!released.value && !opponentReleased.value
                    && (myFrequency > 0f && opponentFrequency > 0f)
                    && (myFrequency - opponentFrequency).absoluteValue < FLOUR_MILL_SYNC_FREQUENCY_THRESHOLD) {
                    val avgFrequency = (myFrequency + opponentFrequency) / 2f
                    val angleChange = RADIANS_IN_CIRCLE * avgFrequency
                    val direction = if(myArc.direction < 0 && opponentArc.direction < 0) -1 else 1
                    _targetWheelAngle.value += angleChange * direction
                    if(!inSync){
                        inSync = true
                        addEvent(SessionEvent.syncStartTime(playerId, timestamp))
                    }
                } else {
                    if(inSync){
                        inSync = false
                        addEvent(SessionEvent.syncEndTime(playerId, timestamp))
                    }
                }
            }
        }

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
        FLOUR_MILL_SYNC_FREQUENCY_THRESHOLD = syncTolerance.toFloat() * 0.01f
        FREQUENCY_HISTORY_MILLISECONDS = syncWindowLength
        Log.d(TAG, "syncTolerance: $syncTolerance")
        Log.d(TAG, "syncWindowLength: $syncWindowLength")
        Log.d(TAG, "My color: $myColor")
        model.sessionSetupComplete()
    }

    open fun setTouchPoint(x: Float, y: Float) {
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
            addEvent(SessionEvent.angle(playerId,timestamp,angle.toString()))
            addEvent(SessionEvent.frequency(playerId,timestamp,frequency.toString()))
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
                addEvent(SessionEvent.opponentFrequency(playerId,arrivedTimestamp,frequency.toString()))
            }
            else -> super.handleGameAction(action)
        }
    }

    override fun setupWavPlayer(){
        try{
            wavPlayer.load(MILL_SOUND_TRACK, R.raw.mill_sound)
        } catch (e: IllegalArgumentException){
            val msg = e.message ?: "Unknown error"
            exitWithError(msg, Result.Code.BAD_RESOURCE)
        }
    }

    companion object {
        private const val TAG = "FlourMillViewModel"
    }
}