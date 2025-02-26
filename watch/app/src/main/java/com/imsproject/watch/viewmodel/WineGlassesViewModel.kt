package com.imsproject.watch.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.fastCoerceIn
import androidx.lifecycle.viewModelScope
import com.imsproject.common.gameserver.GameAction
import com.imsproject.common.gameserver.GameType
import com.imsproject.common.gameserver.SessionEvent
import com.imsproject.watch.ACTIVITY_DEBUG_MODE
import com.imsproject.watch.ARC_DEFAULT_ALPHA
import com.imsproject.watch.FREQUENCY_HISTORY_MILLISECONDS
import com.imsproject.watch.HIGH_LOOP_TRACK
import com.imsproject.watch.INNER_TOUCH_POINT
import com.imsproject.watch.LOW_BUILD_IN_TRACK
import com.imsproject.watch.LOW_BUILD_OUT_TRACK
import com.imsproject.watch.LOW_LOOP_TRACK
import com.imsproject.watch.MAX_ANGLE_SKEW
import com.imsproject.watch.MIN_ANGLE_SKEW
import com.imsproject.watch.MY_SWEEP_ANGLE
import com.imsproject.watch.OUTER_TOUCH_POINT
import com.imsproject.watch.PACKAGE_PREFIX
import com.imsproject.watch.R
import com.imsproject.watch.UNDEFINED_ANGLE
import com.imsproject.watch.WINE_GLASSES_SYNC_FREQUENCY_THRESHOLD
import com.imsproject.watch.utils.FrequencyTracker
import com.imsproject.watch.utils.WavPlayer
import com.imsproject.watch.utils.addToAngle
import com.imsproject.watch.utils.calculateAngleDiff
import com.imsproject.watch.utils.cartesianToPolar
import com.imsproject.watch.utils.isBetweenInclusive
import com.imsproject.watch.utils.isClockwise
import com.imsproject.watch.view.contracts.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue


private const val DIRECTION_MAX_OFFSET = 1.0f

class WineGlassesViewModel : GameViewModel(GameType.WINE_GLASSES) {

    class Arc{
        var startAngle by mutableFloatStateOf(UNDEFINED_ANGLE)
        var angleSkew = 0f
        var direction = 0f
        var previousAngle by mutableFloatStateOf(UNDEFINED_ANGLE)
        var previousAngleDiff = 0f
        var currentAlpha by mutableFloatStateOf(ARC_DEFAULT_ALPHA)
    }

    lateinit var sound: WavPlayer

    // ================================================================================ |
    // ================================ STATE FIELDS ================================== |
    // ================================================================================ |

    val myArc = Arc()
    val opponentArc = Arc()
    private lateinit var myFrequencyTracker : FrequencyTracker
    private lateinit var opponentFrequencyTracker : FrequencyTracker

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

        if(ACTIVITY_DEBUG_MODE){
            viewModelScope.launch(Dispatchers.Default) {
                while(true) {
                    var angle = 0.0f
                    while(angle < 360 * 15){
                        opponentArc.startAngle = angle
                        opponentFrequencyTracker.addSample(angle)
                        angle += 4
                        delay(16)
                    }
                    _opponentReleased.value = true
                    opponentFrequencyTracker.reset()
                    delay(2000)
                    _opponentReleased.value = false
                }
            }
            return
        }

        setupWavPlayer(context)

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
        WINE_GLASSES_SYNC_FREQUENCY_THRESHOLD = syncTolerance.toFloat() * 0.01f
        FREQUENCY_HISTORY_MILLISECONDS = syncWindowLength
        Log.d(TAG, "syncTolerance: $syncTolerance")
        Log.d(TAG, "syncWindowLength: $syncWindowLength")

        // set up frequency trackers
        myFrequencyTracker = FrequencyTracker()
        opponentFrequencyTracker = FrequencyTracker()

        // start the frequency tracking loop
        viewModelScope.launch(Dispatchers.Default){
            while(true){
                delay(100) // run this loop roughly 10 times per second
                if(inSync){
                    val timestamp = getCurrentGameTime()
                    addEvent(SessionEvent.frequency(playerId, timestamp, myFrequencyTracker.frequency.toString()))
                    addEvent(SessionEvent.opponentFrequency(playerId, timestamp, opponentFrequencyTracker.frequency.toString()))
                }
            }
        }
    }

    fun setTouchPoint(x: Double, y: Double) {
        val (distance,rawAngle) = cartesianToPolar(x, y)
        val inBounds = if(x != -1.0 && y != -1.0){
            distance in INNER_TOUCH_POINT..OUTER_TOUCH_POINT
        } else {
            false // not touching the screen
        }

        if(inBounds){
            updateMyArc(rawAngle)
            _released.value = false
            myFrequencyTracker.addSample(myArc.startAngle)
        } else {
            _released.value = true
            myFrequencyTracker.reset()
        }

        if(ACTIVITY_DEBUG_MODE) return

        // send input to server
        viewModelScope.launch(Dispatchers.IO) {
            val timestamp = getCurrentGameTime()
            val data = if(inBounds) myArc.startAngle.toString() else UNDEFINED_ANGLE.toString()
            model.sendUserInput(timestamp, packetTracker.newPacket(),data)
            addEvent(SessionEvent.angle(playerId,timestamp,data))
        }
    }

    fun inSync() = (
            !released.value && !opponentReleased.value
            && (myFrequencyTracker.frequency - opponentFrequencyTracker.frequency)
                .absoluteValue < WINE_GLASSES_SYNC_FREQUENCY_THRESHOLD
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
                val angle = action.data?.toFloat() ?: run{
                    Log.e(TAG, "handleGameAction: missing data in user input action")
                    return
                }
                val sequenceNumber = action.sequenceNumber ?: run{
                    Log.e(TAG, "handleGameAction: missing sequence number in user input action")
                    return
                }

                val arrivedTimestamp = getCurrentGameTime()

                if(angle == UNDEFINED_ANGLE){
                    _opponentReleased.value = true
                    opponentFrequencyTracker.reset()
                } else {
                    _opponentReleased.value = false
                    opponentArc.startAngle = angle
                    opponentFrequencyTracker.addSample(angle)
                }

                packetTracker.receivedOtherPacket(sequenceNumber)
                addEvent(SessionEvent.opponentAngle(playerId,arrivedTimestamp,angle.toString()))
            }
            else -> super.handleGameAction(action)
        }
    }

    private fun updateMyArc(angle: Float){

        // =========== for current iteration =============== |

        // calculate the skew angle to show the arc ahead of the finger
        // based on the calculations of the previous iteration
        val angleSkew = myArc.angleSkew
        myArc.startAngle = addToAngle(angle,
                myArc.direction.fastCoerceIn(-DIRECTION_MAX_OFFSET, DIRECTION_MAX_OFFSET) * angleSkew
                - MY_SWEEP_ANGLE / 2
        )

        // ============== for next iteration =============== |

        // prepare the skew angle for the next iteration
        val previousAngle = myArc.previousAngle
        var angleDiff = 0f
        if(previousAngle != UNDEFINED_ANGLE){
            angleDiff = calculateAngleDiff(previousAngle, angle)
            val previousAngleDiff = myArc.previousAngleDiff
            val angleDiffDiff = angleDiff - previousAngleDiff
            myArc.angleSkew = if (angleDiffDiff > 1 && angleDiff > 3){
                (angleSkew + angleDiff * 0.75f).fastCoerceAtMost(MAX_ANGLE_SKEW)
            } else if (angleDiffDiff < 1){
                (angleSkew - angleDiff * 0.375f).fastCoerceAtLeast(MIN_ANGLE_SKEW)
            } else {
                angleSkew
            }
        }

        // prepare the direction for the next iteration
        // we add a bit to the max offset to prevent random jitter in the direction
        // we clamp the direction to the max offset when calculating the skewed angle
        if (previousAngle != UNDEFINED_ANGLE){
            val direction = myArc.direction
            myArc.direction = if(isClockwise(previousAngle, angle)){
                (direction + angleDiff * 0.2f).fastCoerceAtMost(DIRECTION_MAX_OFFSET + 0.5f)
            } else if (! isClockwise(previousAngle, angle)){
                (direction - angleDiff * 0.2f).fastCoerceAtLeast(-(DIRECTION_MAX_OFFSET + 0.5f))
            } else {
                direction
            }
            if(myArc.direction.isBetweenInclusive(-WINE_GLASSES_SYNC_FREQUENCY_THRESHOLD,
                    WINE_GLASSES_SYNC_FREQUENCY_THRESHOLD
                )) myArc.angleSkew = MIN_ANGLE_SKEW
        }

        // current angle becomes previous angle for the next iteration
        myArc.previousAngle = angle
        myArc.previousAngleDiff = angleDiff
    }

    private fun setupWavPlayer(context: Context){
        try{
            sound = WavPlayer(context, viewModelScope)
            sound.load(LOW_BUILD_IN_TRACK, R.raw.wine_low_buildin)
            sound.load(LOW_LOOP_TRACK, R.raw.wine_low_loop)
            sound.load(LOW_BUILD_OUT_TRACK, R.raw.wine_low_buildout)
            sound.load(HIGH_LOOP_TRACK, R.raw.wine_high_loop)
        } catch (e: IllegalArgumentException){
            val msg = e.message ?: "Unknown error"
            exitWithError(msg, Result.Code.BAD_RESOURCE)
        }
    }

    override fun onExit(){
        sound.pauseAll()
        sound.releaseAll()
        super.onExit()
    }

    companion object {
        private const val TAG = "WineGlassesViewModel"
    }
}