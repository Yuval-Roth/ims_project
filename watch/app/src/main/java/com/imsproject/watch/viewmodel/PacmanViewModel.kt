package com.imsproject.watch.viewmodel

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.SoundPool
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
import com.imsproject.watch.PACKAGE_PREFIX
import com.imsproject.watch.PACMAN_ROTATION_DURATION
import com.imsproject.watch.PACMAN_ANGLE_STEP
import com.imsproject.watch.PACMAN_MOUTH_OPENING_ANGLE
import com.imsproject.watch.PACMAN_PARTICLE_ANIMATION_MAX_DURATION
import com.imsproject.watch.PACMAN_PARTICLE_ANIMATION_MIN_DURATION
import com.imsproject.watch.PACMAN_PARTICLE_DISTANCE_FROM_CENTER
import com.imsproject.watch.PACMAN_PARTICLE_RADIUS
import com.imsproject.watch.R
import com.imsproject.watch.SCREEN_CENTER
import com.imsproject.watch.utils.closestQuantizedAngle
import com.imsproject.watch.utils.quantizeAngles
import com.imsproject.watch.view.contracts.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.pow

open class PacmanViewModel: GameViewModel(GameType.PACMAN) {

    enum class ParticleState {
        NEW,
        STATIONARY,
        MOVING
    }

    class PacmanParticle(
        topLeft: Offset,
        val direction: Int,
    ){
        var topLeft by mutableStateOf(topLeft)
        var size by mutableStateOf(Size(0f, 0f))
        var animationLength by mutableIntStateOf(-1)
        var state by mutableStateOf(ParticleState.NEW)
        var reward by mutableStateOf(false)
    }

    protected val _animatePacman = MutableStateFlow(true)
    val animatePacman : StateFlow<Boolean> = _animatePacman

    protected val _showLeftSide = MutableStateFlow(true)
    val showLeftSide : StateFlow<Boolean> = _showLeftSide

    protected val _showRightSide = MutableStateFlow(true)
    val showRightSide : StateFlow<Boolean> = _showRightSide

    protected lateinit var soundPool: SoundPool
    protected var flingSoundId : Int = -1

    // ================================================================================ |
    // ================================ STATE FIELDS ================================== |
    // ================================================================================ |

    var pacmanAngle = MutableStateFlow(Angle(0f))
    val rewardAccumulator = Animatable(0f)
    var myDirection = 1
        protected set
    protected val _myParticle = MutableStateFlow<PacmanParticle?>(null)
    val myParticle: StateFlow<PacmanParticle?> = _myParticle

    protected val _otherParticle = MutableStateFlow<PacmanParticle?>(null)
    val otherParticle: StateFlow<PacmanParticle?> = _otherParticle

    // ================================================================================ |
    // ============================ PUBLIC METHODS ==================================== |
    // ================================================================================ |


    override fun onCreate(intent: Intent, context: Context) {
        super.onCreate(intent, context)

        soundPool = SoundPool.Builder().setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build()).setMaxStreams(1).build()
        flingSoundId = soundPool.load(context, R.raw.pacman_eat2, 1)

        if(ACTIVITY_DEBUG_MODE){
            viewModelScope.launch(Dispatchers.Default) {
                startGame()
                delay(1000L)
                _myParticle.value = createNewParticle(myDirection)
                _otherParticle.value = createNewParticle(-myDirection)
                val quantizedAngles = quantizeAngles(PACMAN_ANGLE_STEP)
                val flingAngle = Angle.fromArbitraryAngle(closestQuantizedAngle(360 - PACMAN_MOUTH_OPENING_ANGLE, PACMAN_ANGLE_STEP, quantizedAngles))
                pacmanAngle.collect {
                    if(pacmanAngle.value.floatValue == flingAngle.floatValue){
                        handleFling(500f, -myDirection, true)
                    }
                }
            }
            return
        }
        myDirection = intent.getStringExtra("$PACKAGE_PREFIX.additionalData").let {
            when (it) {
                "left" -> 1
                "right" -> -1
                else -> {
                    exitWithError("Missing or invalid direction data", Result.Code.BAD_REQUEST)
                    return
                }
            }
        }
        Log.d(TAG, "myDirection = $myDirection")
        model.sessionSetupComplete()
    }

    open fun fling(dpPerSec: Float) {
        if (_myParticle.value == null) {
            return
        }

        val pxPerSec = dpPerSec * screenDensity
        val animationLength = mapSpeedToDuration(pxPerSec = pxPerSec)
        // calculate reward based on expected final angle
        val degreesPerMilliSecond = 360f / PACMAN_ROTATION_DURATION
        val targetAngle = Angle(if (myDirection > 0) 180f else 0f)
        val expectedFinalAngle = pacmanAngle.value + degreesPerMilliSecond * animationLength
        val reward = expectedFinalAngle - targetAngle <= PACMAN_MOUTH_OPENING_ANGLE / 2f

        if(ACTIVITY_DEBUG_MODE) {
            handleFling(dpPerSec, myDirection, reward)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val timestamp = super.getCurrentGameTime()
            val data = "${dpPerSec.toInt()},${myDirection},$reward"
            val sequenceNumber = packetTracker.newPacket()
            model.sendUserInput(timestamp, sequenceNumber, data)
            addEvent(SessionEvent.fling(playerId, timestamp, data))
        }
    }

    fun resetParticle(direction: Int) {
        val particle = if (direction == myDirection) {
            _myParticle
        } else {
            _otherParticle
        }
        particle.value = null
        viewModelScope.launch(Dispatchers.Main) {
            delay((PACMAN_ROTATION_DURATION * PACMAN_MOUTH_OPENING_ANGLE / 360f).toLong())
            particle.value = createNewParticle(direction)
        }
    }

    fun playRewardSound() {
        soundPool.play(flingSoundId, 1f, 1f, 1, 0, 1f)
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
                val (dpPerSec, direction, reward) = data.split(",")
                handleFling(dpPerSec.toFloat(), direction.toInt(), reward.toBoolean())

                if(actor == playerId){
                    packetTracker.receivedMyPacket(sequenceNumber)
                } else {
                    packetTracker.receivedOtherPacket(sequenceNumber)
                    addEvent(SessionEvent.opponentFling(playerId, arrivedTimestamp, data))
                }
            }
            else -> super.handleGameAction(action)
        }
    }

    override fun startGame() {
        super.startGame()
        viewModelScope.launch(Dispatchers.Main) {
            delay(1000L)
            _myParticle.value = createNewParticle(myDirection)
            _otherParticle.value = createNewParticle(-myDirection)
        }
    }

    protected fun createNewParticle(direction: Int): PacmanParticle {
        return PacmanParticle(
            topLeft = Offset(
                x = SCREEN_CENTER.x - PACMAN_PARTICLE_RADIUS - direction * PACMAN_PARTICLE_DISTANCE_FROM_CENTER,
                y = SCREEN_CENTER.y - PACMAN_PARTICLE_RADIUS
            ),
            direction = direction
        )
    }

    protected fun handleFling(dpPerSec: Float, direction: Int, reward: Boolean) {
        val particle = if (direction == myDirection) {
            _myParticle.value
        } else {
            _otherParticle.value
        } ?: return
        val pxPerSec = dpPerSec * screenDensity
        val animationLength = mapSpeedToDuration(pxPerSec = pxPerSec)

        particle.animationLength = animationLength
        particle.reward = reward
    }

    private fun mapSpeedToDuration(pxPerSec: Float): Int {
        //TODO: fine tune the mapping function
        val duration = if (pxPerSec <= 750f) {
            PACMAN_PARTICLE_ANIMATION_MAX_DURATION
        } else {
            val v = 750f / pxPerSec
            PACMAN_PARTICLE_ANIMATION_MAX_DURATION * v.pow(1.2f)
        }
        return duration.toInt().coerceIn(PACMAN_PARTICLE_ANIMATION_MIN_DURATION, PACMAN_PARTICLE_ANIMATION_MAX_DURATION)
    }

    companion object {
        private const val TAG = "PacmanViewModel"
    }
}