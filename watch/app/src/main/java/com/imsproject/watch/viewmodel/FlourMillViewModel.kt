package com.imsproject.watch.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.imsproject.common.gameserver.GameAction
import com.imsproject.common.gameserver.GameType
import com.imsproject.common.gameserver.SessionEvent
import com.imsproject.watch.ACTIVITY_DEBUG_MODE
import com.imsproject.watch.AXLE_STARTING_ANGLE
import com.imsproject.watch.BRIGHT_CYAN_COLOR
import com.imsproject.watch.FLOUR_MILL_SYNC_TIME_THRESHOLD
import com.imsproject.watch.LIGHT_GRAY_COLOR
import com.imsproject.watch.PACKAGE_PREFIX
import com.imsproject.watch.RESET_COOLDOWN_WAIT_TIME
import com.imsproject.watch.STRETCH_PEAK
import com.imsproject.watch.utils.Angle
import com.imsproject.watch.utils.toAngle
import com.imsproject.watch.view.contracts.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue

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

    class Axle(startingAngle: Angle, val mySide: AxleSide) {
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

        if(ACTIVITY_DEBUG_MODE) {
            myAxleSide = AxleSide.RIGHT
//            axle = Axle(AXLE_STARTING_ANGLE.toAngle(), myAxleSide)
            viewModelScope.launch(Dispatchers.Default) {
                while (true) {
                    delay(1000)
                    // TODO: add rotation code
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
            val axleAngle = if(myAxleSide == AxleSide.RIGHT) angle + -90f else angle + 90f
            _axle.value = Axle(axleAngle, myAxleSide)
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

    companion object {
        private const val TAG = "FlourMillViewModel"
    }
}