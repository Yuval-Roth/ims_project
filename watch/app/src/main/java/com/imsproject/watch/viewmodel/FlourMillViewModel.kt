package com.imsproject.watch.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.imsproject.common.gameserver.GameAction
import com.imsproject.common.gameserver.GameType
import com.imsproject.common.gameserver.SessionEvent
import com.imsproject.watch.ACTIVITY_DEBUG_MODE
import com.imsproject.watch.FLOUR_MILL_SYNC_TIME_THRESHOLD
import com.imsproject.watch.PACKAGE_PREFIX
import com.imsproject.watch.SCREEN_RADIUS
import com.imsproject.watch.TOUCH_CIRCLE_RADIUS
import com.imsproject.watch.utils.Angle
import com.imsproject.watch.view.contracts.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

class FlourMillViewModel : GameViewModel(GameType.FLOUR_MILL) {

    enum class AxleSide(val angle: Float){
        LEFT(-90f),
        RIGHT(90f);

        fun otherSide() = when(this){
            LEFT -> RIGHT
            RIGHT -> LEFT
        }

        companion object {
            fun fromString(string: String) = when(string.lowercase()){
                "left" -> LEFT
                "right" -> RIGHT
                else -> throw IllegalArgumentException("invalid string")
            }
        }
    }

    class Axle(startingAngle: Angle) {
        var angle by mutableStateOf(startingAngle)
        var targetAngle = startingAngle
    }

    // ================================================================================ |
    // ================================ STATE FIELDS ================================== |
    // ================================================================================ |

    private val _axle = MutableStateFlow<Axle?>(null)
    val axle: StateFlow<Axle?> = _axle

    lateinit var myAxleSide : AxleSide
        private set

    private val _myTouchPoint = MutableStateFlow<Pair<Float,Angle>>(-1f to Angle.undefined())
    /**
     *  <relativeRadius,angle>
     */
    val myTouchPoint: StateFlow<Pair<Float,Angle>> = _myTouchPoint

    private val _opponentTouchPoint = MutableStateFlow<Pair<Float,Angle>>(-1f to Angle.undefined())
    /**
     *  <relativeRadius,angle>
     */
    val opponentTouchPoint: StateFlow<Pair<Float,Angle>> = _opponentTouchPoint

    // ================================================================================ |
    // ============================ PUBLIC METHODS ==================================== |
    // ================================================================================ |

    override fun onCreate(intent: Intent, context: Context) {
        super.onCreate(intent,context)

        viewModelScope.launch {
            while(true){
                delay(16)
                updateAxleAngle()
            }
        }

        if(ACTIVITY_DEBUG_MODE) {
            myAxleSide = AxleSide.RIGHT
            viewModelScope.launch(Dispatchers.Default) {
                while (true) {
                    delay(100)
                    val axle = _axle.value
                    if(axle == null){
                        _opponentTouchPoint.value = -1f to Angle.undefined()
                    } else {
                        val angle = axle.angle + -80f
                        _opponentTouchPoint.value = 0.8f to angle
                    }
                }
            }
            return
        }

        myAxleSide = intent.getStringExtra("$PACKAGE_PREFIX.additionalData")?.let { AxleSide.fromString(it) }!!

        val syncTolerance = intent.getLongExtra("$PACKAGE_PREFIX.syncTolerance", -1)
        if (syncTolerance <= 0L) {
            exitWithError("Missing sync tolerance", Result.Code.BAD_REQUEST)
            return
        }
        FLOUR_MILL_SYNC_TIME_THRESHOLD = syncTolerance
        Log.d(TAG, "syncTolerance: $syncTolerance")
    }

    fun setTouchPoint(relativeRadius: Float, angle: Angle) {
        _myTouchPoint.value = relativeRadius to angle

        if(relativeRadius < 0f){
            _axle.value = null
        } else if (relativeRadius >= 0f && _axle.value == null){

            //TODO: do this in the correct place as it won't be local in the future
            val axleAngle = angle + if(myAxleSide == AxleSide.RIGHT) -AxleSide.RIGHT.angle else AxleSide.LEFT.angle
            _axle.value = Axle(axleAngle)
        }

        //TODO: network calls
    }

    fun isTouchPointInbounds(side: AxleSide): Boolean {
        val axle = _axle.value ?: return false

        val touchPoint = if(side == myAxleSide) _myTouchPoint.value else _opponentTouchPoint.value
        if(touchPoint.first < 0f) return false

        val touchPointDistance = touchPoint.first * SCREEN_RADIUS
        val touchPointAngle = touchPoint.second
        val sideAngle = axle.angle + if(side == AxleSide.LEFT) AxleSide.LEFT.angle else AxleSide.RIGHT.angle
        return (touchPointAngle - sideAngle <= (360f / TOUCH_CIRCLE_RADIUS))
                && touchPointDistance > (SCREEN_RADIUS * 0.7f - TOUCH_CIRCLE_RADIUS)
                && touchPointDistance < (SCREEN_RADIUS * 0.9f + TOUCH_CIRCLE_RADIUS)
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
                val direction = action.data?.toInt() ?: run{
                    Log.e(TAG, "handleGameAction: missing data in user input action")
                    return
                }
                val sequenceNumber = action.sequenceNumber ?: run{
                    Log.e(TAG, "handleGameAction: missing sequence number in user input action")
                    return
                }

                val arrivedTimestamp = getCurrentGameTime()

                // TODO: handle user input

                if(actor == playerId){
                    packetTracker.receivedMyPacket(sequenceNumber)
                } else {
                    packetTracker.receivedOtherPacket(sequenceNumber)
                    addEvent(SessionEvent.opponentRotation(playerId,arrivedTimestamp,direction.toString()))
                }
            }
            else -> super.handleGameAction(action)
        }
    }

    private fun updateAxleAngle(){
        val axle = _axle.value ?: return
        if(isTouchPointInbounds(AxleSide.LEFT) && isTouchPointInbounds(AxleSide.RIGHT)){
            val myAngle = _myTouchPoint.value.second
            val opponentAngle = _opponentTouchPoint.value.second
            val mySideAngle = axle.angle + myAxleSide.angle
            val opponentSideAngle = axle.angle + myAxleSide.otherSide().angle
            val myAngleDiff = myAngle - mySideAngle
            val opponentAngleDiff = opponentAngle - opponentSideAngle
            val myDirection = if(Angle.isClockwise(mySideAngle,myAngle)) 1 else -1
            val opponentDirection = if(Angle.isClockwise(opponentSideAngle, opponentAngle)) 1 else -1
            if(myAngleDiff > 0 && opponentAngleDiff > 0 && myDirection == opponentDirection){
                val amountToRotate = abs(myAngleDiff - opponentAngleDiff)
                axle.targetAngle = axle.targetAngle + (amountToRotate * myDirection)
            }
        }
    }

    companion object {
        private const val TAG = "FlourMillViewModel"
    }
}