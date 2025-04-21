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
import com.imsproject.common.utils.Angle
import com.imsproject.common.utils.UNDEFINED_ANGLE
import com.imsproject.watch.AXLE_WIDTH
import com.imsproject.watch.TURNING_BONUS_THRESHOLD
import com.imsproject.watch.utils.PacketTracker
import com.imsproject.watch.utils.polarDistance
import com.imsproject.watch.utils.toAngle
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

    private val _myTouchPoint = MutableStateFlow<Pair<Float,Angle>>(-1f to Angle.undefined)
    /**
     *  <relativeRadius,angle>
     */
    val myTouchPoint: StateFlow<Pair<Float,Angle>> = _myTouchPoint

    private val _opponentTouchPoint = MutableStateFlow<Pair<Float,Angle>>(-1f to Angle.undefined)
    /**
     *  <relativeRadius,angle>
     */
    val opponentTouchPoint: StateFlow<Pair<Float,Angle>> = _opponentTouchPoint

    private val _myInBounds = MutableStateFlow(false)
    val myInBounds: StateFlow<Boolean> = _myInBounds

    private val _opponentInBounds = MutableStateFlow(false)
    val opponentInBounds: StateFlow<Boolean> = _opponentInBounds

    private var turning = false

    private var myFirstTouch = -1L
    private var opponentFirstTouch = -1L

    // ================================================================================ |
    // ============================ PUBLIC METHODS ==================================== |
    // ================================================================================ |

    override fun onCreate(intent: Intent, context: Context) {
        super.onCreate(intent,context)

        if(ACTIVITY_DEBUG_MODE) {
            myAxleSide = AxleSide.RIGHT
            viewModelScope.launch {
                while(true){
                    delay(16)
                    _myInBounds.value = isTouchPointInbounds()
                    updateAxleAngle()
                }
            }
            viewModelScope.launch(Dispatchers.Default) {
                while (true) {
                    delay(100)
                    val axle = _axle.value
                    if(axle == null){
                        _opponentTouchPoint.value = -1f to Angle.undefined
                        _opponentInBounds.value = false
                    } else {
                        val angle = axle.angle + -80f
                        _opponentTouchPoint.value = 0.8f to angle
                        _opponentInBounds.value = true
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

        viewModelScope.launch {
            while(true){
                delay(16)
                updateAxleAngle()
                val touchPointInbounds = isTouchPointInbounds()
                if(touchPointInbounds != _myInBounds.value){
                    sendCurrentState()
                }
                _myInBounds.value = touchPointInbounds
            }
        }
    }

    fun setTouchPoint(relativeRadius: Float, angle: Angle) {
        val firstTouch = _myTouchPoint.value.first < 0f && relativeRadius >= 0f
        if(firstTouch){
            myFirstTouch = getCurrentGameTime()
        }

        _myTouchPoint.value = relativeRadius to angle
        if(firstTouch){
            updateAxleAngle()
        }

        val touchPointInbounds = isTouchPointInbounds()
        _myInBounds.value = touchPointInbounds

        if(ACTIVITY_DEBUG_MODE){
            if(relativeRadius > 0 && _axle.value == null){
                _axle.value = Axle(angle + -myAxleSide.angle)
            } else if(relativeRadius < 0 && _axle.value != null){
                _axle.value = null
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            sendCurrentState()
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
                val data = action.data?.split(",") ?: run{
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

                val relativeRadius = data[0].toFloat()
                val angle = data[1].toFloat().toAngle()
                val inBounds = data[2].toBoolean()

                if(_opponentTouchPoint.value.first < 0f && relativeRadius >= 0f){
                    opponentFirstTouch = timestamp
                }

                _opponentTouchPoint.value = relativeRadius to angle
                _opponentInBounds.value = inBounds

                addEvent(SessionEvent.opponentTouchPoint(actor,arrivedTimestamp,"$relativeRadius,$angle,$inBounds"))
            }
            else -> super.handleGameAction(action)
        }
    }

    private fun isTouchPointInbounds(): Boolean {
        val axle = _axle.value ?: return false

        val touchPoint = _myTouchPoint.value
        if(touchPoint.first < 0f) return false

        val touchPointDistance = touchPoint.first * SCREEN_RADIUS
        val touchPointAngle = touchPoint.second
        val sideAngle = axle.angle + myAxleSide.angle
        val polarDistance = polarDistance(touchPointDistance, sideAngle, touchPointDistance, touchPointAngle)
        val bonusThreshold = TURNING_BONUS_THRESHOLD * if(turning) 1 else 0
        return ((polarDistance <= TOUCH_CIRCLE_RADIUS + AXLE_WIDTH / 2f + bonusThreshold)
                && touchPointDistance > (SCREEN_RADIUS * 0.7f - TOUCH_CIRCLE_RADIUS)
                && touchPointDistance < (SCREEN_RADIUS * 0.9f + TOUCH_CIRCLE_RADIUS)).also{
                    if(!it) turning = false
        }
    }

    private fun updateAxleAngle(){

        val timestamp = super.getCurrentGameTime()

        val axle = _axle.value
        val myTouchPoint = _myTouchPoint.value
        val opponentTouchPoint = _opponentTouchPoint.value
        if(axle == null){
            val angle = if(myTouchPoint.first >= 0f && opponentTouchPoint.first >= 0f) {
                if(myFirstTouch > opponentFirstTouch) {
                    myTouchPoint.second + -myAxleSide.angle
                } else {
                    opponentTouchPoint.second + -myAxleSide.otherSide().angle
                }
            } else if(myTouchPoint.first >= 0f) {
                myTouchPoint.second + -myAxleSide.angle
            } else if(opponentTouchPoint.first >= 0f) {
                opponentTouchPoint.second + -myAxleSide.otherSide().angle
            } else {
                Angle(UNDEFINED_ANGLE)
            }
            if(angle != Angle.undefined) {
                _axle.value = Axle(angle)
                turning = false
                addEvent(SessionEvent.axleAngle(playerId,timestamp,angle.floatValue.toString()))
            }
        } else if (myTouchPoint.first >= 0f && opponentTouchPoint.first >= 0f){
            if(myInBounds.value && opponentInBounds.value){
                val myAngle = myTouchPoint.second
                val opponentAngle = opponentTouchPoint.second
                val axleAngle = axle.angle
                val mySideAngle = axleAngle + myAxleSide.angle
                val opponentSideAngle = axleAngle + myAxleSide.otherSide().angle
                val myAngleDiff = myAngle - mySideAngle
                val opponentAngleDiff = opponentAngle - opponentSideAngle
                val myDirection = if(Angle.isClockwise(mySideAngle,myAngle)) 1 else -1
                val opponentDirection = if(Angle.isClockwise(opponentSideAngle, opponentAngle)) 1 else -1
                if(myAngleDiff > 0 && opponentAngleDiff > 0 && myDirection == opponentDirection){
                    val amountToRotate = abs(myAngleDiff - opponentAngleDiff)
                    val newTargetAngle = axleAngle + (amountToRotate * myDirection)
                    axle.targetAngle = newTargetAngle
                    turning = true
                    addEvent(SessionEvent.axleAngle(playerId,timestamp,newTargetAngle.toString()))
                }
            }
        } else if (myTouchPoint.first < 0f && opponentTouchPoint.first < 0f){
            _axle.value = null
            turning = false
            addEvent(SessionEvent.axleAngle(playerId,timestamp,UNDEFINED_ANGLE.toString()))
        }
    }

    private fun sendCurrentState(){
        val timestamp = super.getCurrentGameTime()
        val touchPointInbounds = _myInBounds.value
        val (relativeRadius, angle) = _myTouchPoint.value
        val data = "$relativeRadius,$angle,$touchPointInbounds"
        model.sendUserInput(timestamp, packetTracker.newPacket(),data)
        addEvent(SessionEvent.touchPoint(playerId,timestamp,data))
    }

    companion object {
        private const val TAG = "FlourMillViewModel"
    }
}