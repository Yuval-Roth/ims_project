package com.imsproject.watch.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.lifecycle.viewModelScope
import com.imsproject.common.gameserver.GameAction
import com.imsproject.common.gameserver.GameType
import com.imsproject.common.gameserver.SessionEvent
import com.imsproject.watch.ACTIVITY_DEBUG_MODE
import com.imsproject.watch.SCREEN_CENTER
import com.imsproject.watch.SCREEN_RADIUS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.pow

class PacmanViewModel: GameViewModel(GameType.PACMAN) {

    enum class ParticleState {
        NEW,
        STATIONARY,
        MOVING
    }

    class Particle(
        topLeft: Offset,
        val direction: Int,
    ){
        var topLeft by mutableStateOf(topLeft)
        var size by mutableStateOf(Size(0f, 0f))
        var animationLength by mutableIntStateOf(-1)
        var state by mutableStateOf(ParticleState.NEW)
    }

    // ================================================================================ |
    // ================================ STATE FIELDS ================================== |
    // ================================================================================ |

    var myDirection = 1
        private set
    private val _myParticle = MutableStateFlow<Particle?>(null)
    val myParticle: StateFlow<Particle?> = _myParticle

    private val _otherParticle = MutableStateFlow<Particle?>(null)
    val otherParticle: StateFlow<Particle?> = _otherParticle

    // ================================================================================ |
    // ============================ PUBLIC METHODS ==================================== |
    // ================================================================================ |


    override fun onCreate(intent: Intent, context: Context) {
        super.onCreate(intent, context)

        viewModelScope.launch {
            delay(500L)
            _myParticle.value = createNewParticle(myDirection)
            _otherParticle.value = createNewParticle(-myDirection)
        }


        if(ACTIVITY_DEBUG_MODE){
            viewModelScope.launch(Dispatchers.Default) {

            }
            return
        }
    }

    fun fling(dpPerSec: Float, direction: Int){
        require(direction != 0) { "direction must be positive or negative" }
        val animationLength = mapSpeedToDuration(pxPerSec = dpPerSec, minDurationMs = 150, maxDurationMs = 750)
        if (ACTIVITY_DEBUG_MODE){
            val myParticle = _myParticle.value ?: return
            myParticle.animationLength = animationLength
        }
    }

    fun resetParticle(direction: Int) {
        val newParticle = createNewParticle(direction)
        if (direction == myDirection) {
            _myParticle.value = newParticle
        } else {
            _otherParticle.value = newParticle
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
                // TODO: handle data received

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

    private fun createNewParticle(direction: Int): Particle {
        val particleRadius = (SCREEN_RADIUS * 0.02f)
        val particleDistanceFromCenter = SCREEN_RADIUS * 0.88f
        return Particle(
            topLeft = Offset(
                x = SCREEN_CENTER.x - particleRadius - direction * particleDistanceFromCenter,
                y = SCREEN_CENTER.y - particleRadius
            ),
            direction = direction
        )
    }

    private fun mapSpeedToDuration(pxPerSec: Float, minDurationMs: Int, maxDurationMs: Int): Int {
        val duration = if (pxPerSec <= 750f) {
            maxDurationMs
        } else {
            val v = 750f / pxPerSec
            val duration = maxDurationMs * v.pow(1.5f)
            println(duration)
            duration.toInt().coerceIn(minDurationMs, maxDurationMs)
        }
        return duration
    }

    companion object {
        private const val TAG = "PacmanViewModel"
    }
}