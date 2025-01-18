package com.imsproject.watch.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.lifecycle.viewModelScope
import com.imsproject.common.gameserver.GameAction
import com.imsproject.common.gameserver.GameType
import com.imsproject.common.gameserver.SessionEvent
import com.imsproject.watch.ACTIVITY_DEBUG_MODE
import com.imsproject.watch.UNDEFINED_ANGLE
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.imsproject.watch.ARC_DEFAULT_ALPHA
import com.imsproject.watch.INNER_TOUCH_POINT
import com.imsproject.watch.MAX_ANGLE_SKEW
import com.imsproject.watch.MIN_ANGLE_SKEW
import com.imsproject.watch.MY_SWEEP_ANGLE
import com.imsproject.watch.OUTER_TOUCH_POINT
import com.imsproject.watch.WINE_GLASSES_SYNC_FREQUENCY_THRESHOLD
import com.imsproject.watch.utils.FrequencyTracker
import com.imsproject.watch.utils.addToAngle
import com.imsproject.watch.utils.cartesianToPolar
import com.imsproject.watch.utils.calculateAngleDiff
import com.imsproject.watch.utils.isBetweenInclusive
import com.imsproject.watch.utils.isClockwise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
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

    // ================================================================================ |
    // ================================ STATE FIELDS ================================== |
    // ================================================================================ |

    val myArc = Arc()
    val opponentArc = Arc()
    private val myFrequencyTracker = FrequencyTracker()
    private val opponentFrequencyTracker = FrequencyTracker()

    private var _released = MutableStateFlow(true)
    val released : StateFlow<Boolean> = _released

    private var _opponentReleased = MutableStateFlow(false)
    val opponentReleased : StateFlow<Boolean> = _opponentReleased

    private var inSync = false
        set(value) {
            if(field != value){
                val timestamp = getCurrentGameTime()
                viewModelScope.launch(Dispatchers.IO) {
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
            viewModelScope.launch(Dispatchers.IO) {
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
                myArc.direction.coerceIn(-DIRECTION_MAX_OFFSET, DIRECTION_MAX_OFFSET) * angleSkew
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
                (angleSkew + angleDiff * 0.75f).coerceAtMost(MAX_ANGLE_SKEW)
            } else if (angleDiffDiff < 1){
                (angleSkew - angleDiff * 0.375f).coerceAtLeast(MIN_ANGLE_SKEW)
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
                (direction + angleDiff * 0.2f).coerceAtMost(DIRECTION_MAX_OFFSET + 0.5f)
            } else if (! isClockwise(previousAngle, angle)){
                (direction - angleDiff * 0.2f).coerceAtLeast(-(DIRECTION_MAX_OFFSET + 0.5f))
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

    companion object {
        private const val TAG = "WineGlassesViewModel"
    }
}