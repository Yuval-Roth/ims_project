package com.imsproject.watch.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.lifecycle.viewModelScope
import com.imsproject.common.gameserver.GameAction
import com.imsproject.common.gameserver.GameType
import com.imsproject.common.gameserver.SessionEvent
import com.imsproject.common.utils.Angle
import com.imsproject.watch.ACTIVITY_DEBUG_MODE
import com.imsproject.watch.ANGLE_ROTATION_DURATION
import com.imsproject.watch.PACMAN_MOUTH_OPENING_ANGLE
import com.imsproject.watch.PARTICLE_DISTANCE_FROM_CENTER
import com.imsproject.watch.PARTICLE_RADIUS
import com.imsproject.watch.SCREEN_CENTER
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
        var reward by mutableStateOf(false)
    }

    // ================================================================================ |
    // ================================ STATE FIELDS ================================== |
    // ================================================================================ |

    var pacmanAngle = mutableStateOf(Angle(0f))
    val rewardAccumulator = Animatable(0f)
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
                while(true){
                    if(pacmanAngle.value.floatValue == 0f){
                        _otherParticle.value?.animationLength = 150
                        _otherParticle.value?.reward = true
                    }
                }
            }
            return
        }
    }

    fun fling(dpPerSec: Float, currentOpeningAngle: Angle) {
        val animationLength = mapSpeedToDuration(pxPerSec = dpPerSec, minDurationMs = 150, maxDurationMs = 750)
        val degreesPerMilliSecond = 360f / ANGLE_ROTATION_DURATION
        val targetAngle = Angle(if (myDirection > 0) 180f else 0f)
        val expectedFinalAngle = currentOpeningAngle + degreesPerMilliSecond * animationLength
        val reward = expectedFinalAngle - targetAngle <= PACMAN_MOUTH_OPENING_ANGLE

        if (ACTIVITY_DEBUG_MODE){
            val myParticle = _myParticle.value ?: return
            myParticle.animationLength = animationLength
            myParticle.reward = reward
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
        return Particle(
            topLeft = Offset(
                x = SCREEN_CENTER.x - PARTICLE_RADIUS - direction * PARTICLE_DISTANCE_FROM_CENTER,
                y = SCREEN_CENTER.y - PARTICLE_RADIUS
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
            duration.toInt().coerceIn(minDurationMs, maxDurationMs)
        }
        return duration
    }

    companion object {
        private const val TAG = "PacmanViewModel"
    }
}