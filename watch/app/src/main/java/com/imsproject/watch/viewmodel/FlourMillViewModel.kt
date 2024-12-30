package com.imsproject.watch.viewmodel

import android.content.Intent
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.imsproject.common.gameServer.GameAction
import com.imsproject.common.gameServer.GameRequest
import com.imsproject.common.gameServer.GameType
import com.imsproject.watch.ACTIVITY_DEBUG_MODE
import com.imsproject.watch.AXLE_STARTING_ANGLE
import com.imsproject.watch.BRIGHT_CYAN_COLOR
import com.imsproject.watch.FLOUR_MILL_SYNC_TIME_THRESHOLD
import com.imsproject.watch.LIGHT_GRAY_COLOR
import com.imsproject.watch.PACKAGE_PREFIX
import com.imsproject.watch.STRETCH_PEAK
import com.imsproject.watch.model.Position
import com.imsproject.watch.utils.WaitMonitor
import com.imsproject.watch.utils.addToAngle
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.absoluteValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    class AxleEnd(val handleColor: Color, startingAngle: Float){
        var stretchPointAngle by mutableFloatStateOf(startingAngle)
        var stretchPeak = 0.0f
        var direction = 0
    }

    class Axle(startingAngle: Float, mySide: AxleSide) {
        var angle by mutableFloatStateOf(startingAngle)
        // The axle ends are animated based on the effective angle
        // the effective angle only changes about the axle finishes rotating
        var effectiveAngle = startingAngle
        // ================================= |
        val leftEnd : AxleEnd
        val rightEnd : AxleEnd
        var toRotateDirection = 0

        init {
            val leftColor = when(mySide){
                AxleSide.LEFT -> BRIGHT_CYAN_COLOR
                AxleSide.RIGHT -> LIGHT_GRAY_COLOR
            }
            val rightColor = when(mySide){
                AxleSide.LEFT -> LIGHT_GRAY_COLOR
                AxleSide.RIGHT -> BRIGHT_CYAN_COLOR
            }
            leftEnd = AxleEnd(leftColor,getEndAngle(AxleSide.LEFT))
            rightEnd = AxleEnd(rightColor,getEndAngle(AxleSide.RIGHT))
        }

        fun getEndAngle(endSide : AxleSide) = addToAngle(angle, endSide.angle)
        fun getEffectiveEndAngle(endSide : AxleSide) = addToAngle(effectiveAngle, endSide.angle)
        fun getEnd(endSide: AxleSide) = when(endSide){
            AxleSide.LEFT -> leftEnd
            AxleSide.RIGHT -> rightEnd
        }
    }

    class Rotation(val direction : Int) : Position {
        init {
            if(direction != 1 && direction != -1) throw IllegalArgumentException("direction must be either 1 or -1")
        }
        override fun toString(): String {
            return direction.toString()
        }
        companion object {
            fun fromString(string: String) = Rotation(string.toInt())
        }
    }

    // ================================================================================ |
    // ================================ STATE FIELDS ================================== |
    // ================================================================================ |

    lateinit var axle : Axle
        private set
    lateinit var myAxleSide : AxleSide
        private set

    val leftMonitor = WaitMonitor(viewModelScope)
    val rightMonitor = WaitMonitor(viewModelScope)

    private var _leftCounter = MutableStateFlow(0)
    val leftCounter : StateFlow<Int> = _leftCounter

    private var _rightCounter = MutableStateFlow(0)
    val rightCounter : StateFlow<Int> = _rightCounter

    private var _axleCounter = MutableStateFlow(0)
    val axleCounter : StateFlow<Int> = _axleCounter

    private var _coolingDown = MutableStateFlow(false)
    val coolingDown : StateFlow<Boolean> = _coolingDown

    private var leftSyncStatusObserved = true
    private var rightSyncStatusObserved = true
    private var leftLastRotation =  0L
    private var rightLastRotation = 0L

    // ================================================================================ |
    // ============================ PUBLIC METHODS ==================================== |
    // ================================================================================ |

    fun cooledDown(){
        _coolingDown.value = false
    }

    fun isSynced(side: AxleSide) = when(side){
        AxleSide.LEFT -> {
            val inSync = !leftSyncStatusObserved
            leftSyncStatusObserved = true
            if (inSync) {
                if (rightSyncStatusObserved) {
                    _axleCounter.value++
                } else {
                    rightMonitor.wakeup()
                }
            }
            inSync
        }
        AxleSide.RIGHT -> {
            val inSync = !rightSyncStatusObserved
            rightSyncStatusObserved = true
            if (inSync) {
                if (leftSyncStatusObserved) {
                    _axleCounter.value++
                } else {
                    leftMonitor.wakeup()
                }
            }
            inSync
        }
    }

    fun rotateMyAxleEnd(direction: Int){
        _coolingDown.value = true

        if(ACTIVITY_DEBUG_MODE){
            rotateAxle(myAxleSide, direction, System.currentTimeMillis())
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            model.sendPosition(Rotation(direction), super.getCurrentGameTime())
        }
    }

    override fun onCreate(intent: Intent) {
        super.onCreate(intent)

        if(ACTIVITY_DEBUG_MODE) {
            myAxleSide = AxleSide.RIGHT
            axle = Axle(AXLE_STARTING_ANGLE, myAxleSide)
            viewModelScope.launch(Dispatchers.IO) {
                while (true) {
                    delay(1000)
                    rotateAxle(AxleSide.LEFT, 1, System.currentTimeMillis())
                }
            }
            return
        }

        myAxleSide = intent.getStringExtra("$PACKAGE_PREFIX.additionalData")?.let { AxleSide.fromString(it) }!!
        axle = Axle(AXLE_STARTING_ANGLE, myAxleSide)
    }

    // ================================================================================ |
    // ============================ PRIVATE METHODS =================================== |
    // ================================================================================ |

    /**
     * handles game actions
     */
    override suspend fun handleGameAction(action: GameAction) {
        when (action.type) {
            GameAction.Type.POSITION -> {
                val actor = action.actor ?: run{
                    Log.e(TAG, "handleGameAction: missing actor in position action")
                    return
                }
                val timestamp = action.timestamp?.toLong() ?: run{
                    Log.e(TAG, "handleGameAction: missing timestamp in position action")
                    return
                }
                val rotation = action.data?.let { Rotation.fromString(it) } ?: run{
                    Log.e(TAG, "handleGameAction: missing position in position action")
                    return
                }
                withContext(Dispatchers.Main) {
                    if (actor == playerId) {
                        rotateAxle(myAxleSide, rotation.direction, timestamp)
                    } else {
                        rotateAxle(myAxleSide.otherSide(), rotation.direction, timestamp)
                    }
                }
            }
            else -> super.handleGameAction(action)
        }
    }

    /**
     * handles game requests
     */
    override suspend fun handleGameRequest(request: GameRequest) {
        when (request.type) {
            GameRequest.Type.END_GAME -> exitOk()
            else -> super.handleGameRequest(request)
        }
    }

    private fun rotateAxle(axleSide: AxleSide, direction: Int, timestamp: Long){
        if(direction == 0) throw IllegalArgumentException("direction must be either 1 or -1")
        val end = axle.getEnd(axleSide)
        end.direction = direction
        end.stretchPeak = STRETCH_PEAK

        val sameDirection = axle.leftEnd.direction == axle.rightEnd.direction

        when(axleSide){
            AxleSide.LEFT ->{
                leftLastRotation = timestamp
                _leftCounter.value++

                if((timestamp - rightLastRotation).absoluteValue < FLOUR_MILL_SYNC_TIME_THRESHOLD
                    && sameDirection){
                    setInSync(direction)
                }
            }
            AxleSide.RIGHT ->{
                rightLastRotation = timestamp
                _rightCounter.value++

                if((timestamp - leftLastRotation).absoluteValue < FLOUR_MILL_SYNC_TIME_THRESHOLD
                    && sameDirection){
                    setInSync(direction)
                }
            }
        }
    }

    private fun setInSync(direction: Int) {
        axle.toRotateDirection = direction
        leftSyncStatusObserved = false
        rightSyncStatusObserved = false
    }

    companion object {
        private const val TAG = "FlourMillViewModel"
    }
}