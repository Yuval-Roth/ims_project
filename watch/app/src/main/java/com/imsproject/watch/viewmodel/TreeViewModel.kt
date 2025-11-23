package com.imsproject.watch.viewmodel

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.VibrationEffect
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.viewModelScope
import com.imsproject.common.gameserver.GameAction
import com.imsproject.common.gameserver.GameType
import com.imsproject.common.gameserver.SessionEvent
import com.imsproject.watch.utils.Angle
import com.imsproject.watch.ACTIVITY_DEBUG_MODE
import com.imsproject.watch.PACKAGE_PREFIX
import com.imsproject.watch.TREE_RING_ANGLE_STEP
import com.imsproject.watch.R
import com.imsproject.watch.SCREEN_CENTER
import com.imsproject.watch.TREE_PARTICLE_ANIMATION_MAX_DURATION
import com.imsproject.watch.TREE_PARTICLE_ANIMATION_MIN_DURATION
import com.imsproject.watch.TREE_PARTICLE_DISTANCE_FROM_CENTER
import com.imsproject.watch.TREE_PARTICLE_RELATIVE_DISTANCE_FROM_CENTER
import com.imsproject.watch.TREE_RING_OPENING_ANGLE
import com.imsproject.watch.TREE_RING_RELATIVE_RADIUS
import com.imsproject.watch.TREE_RING_ROTATION_DURATION
import com.imsproject.watch.utils.closestQuantizedAngle
import com.imsproject.watch.utils.quantizeAngles
import com.imsproject.watch.view.contracts.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.pow

open class TreeViewModel: GameViewModel(GameType.TREE) {

    enum class ParticleState {
        NEW,
        STATIONARY,
        MOVING
    }

    class TreeParticle(
        center: Offset,
        val direction: Int,
    ){
        var alpha by mutableFloatStateOf(0f)
        var center by mutableStateOf(center)
        var animationLength by mutableIntStateOf(-1)
        var state by mutableStateOf(ParticleState.NEW)
        var success by mutableStateOf(false)
    }

    protected val _animateRing = MutableStateFlow(true)
    val animateRing : StateFlow<Boolean> = _animateRing

    protected val _showLeftSide = MutableStateFlow(true)
    val showLeftSide : StateFlow<Boolean> = _showLeftSide

    protected val _showRightSide = MutableStateFlow(true)
    val showRightSide : StateFlow<Boolean> = _showRightSide

    protected lateinit var soundPool: SoundPool
    protected var rewardSoundId : Int = -1

    private lateinit var clickVibration : VibrationEffect

    // ================================================================================ |
    // ================================ STATE FIELDS ================================== |
    // ================================================================================ |

    val ringAngle = MutableStateFlow(0f)
    var myDirection = 1
        protected set

    protected val _myParticle = MutableStateFlow<TreeParticle?>(null)
    val myParticle: StateFlow<TreeParticle?> = _myParticle

    protected val _otherParticle = MutableStateFlow<TreeParticle?>(null)
    val otherParticle: StateFlow<TreeParticle?> = _otherParticle

    // ================================================================================ |
    // ============================ PUBLIC METHODS ==================================== |
    // ================================================================================ |


    override fun onCreate(intent: Intent, context: Context) {
        super.onCreate(intent, context)

        soundPool = SoundPool.Builder().setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build()).setMaxStreams(1).build()
        // TODO: replace with new sound
        rewardSoundId = soundPool.load(context, R.raw.tree_pop1, 1)

        clickVibration = VibrationEffect.createWaveform(
            longArrayOf(0, 20,10,10,10),
            intArrayOf(0, 200,120,100,80),
            -1
        )

        if(ACTIVITY_DEBUG_MODE){
            viewModelScope.launch(Dispatchers.Default) {
                startGame()
                delay(1000L)
                _myParticle.value = createNewParticle(myDirection)
                _otherParticle.value = createNewParticle(-myDirection)
                val quantizedAngles = quantizeAngles(TREE_RING_ANGLE_STEP)
                val flingAngle = closestQuantizedAngle(360f - TREE_RING_OPENING_ANGLE*0.5f, TREE_RING_ANGLE_STEP, quantizedAngles)
                ringAngle.collect {
                    if(ringAngle.value == flingAngle){
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
        viewModelScope.launch(Dispatchers.IO) {
            model.sessionSetupComplete()
        }
    }

    fun vibrateClick(){
        vibrator.vibrate(clickVibration)
    }

    open fun fling(dpPerSec: Float) {
        if (_myParticle.value == null) {
            return
        }

        val pxPerSec = dpPerSec * screenDensity
        val animationLength = mapSpeedToDuration(pxPerSec = pxPerSec)
        // calculate reward based on expected final angle
        val degreesPerMilliSecond = 360f / TREE_RING_ROTATION_DURATION
        val targetAngle = Angle(if (myDirection > 0) 180f else 0f)


//        val ringWidth: Float = radius * 0.08f
//        val innerRadius = radius * 0.93f
//        val innerRingWidth = innerRadius * 0.08f

        val relativeDistance = (TREE_PARTICLE_RELATIVE_DISTANCE_FROM_CENTER - TREE_RING_RELATIVE_RADIUS) / TREE_PARTICLE_RELATIVE_DISTANCE_FROM_CENTER

        val delta = degreesPerMilliSecond * animationLength * relativeDistance
        val expectedFinalAngle = (ringAngle.value + delta) % 360f
        val reward = Angle.fromArbitraryAngle(expectedFinalAngle) - targetAngle <= TREE_RING_OPENING_ANGLE / 2f

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
            delay((TREE_RING_ROTATION_DURATION * TREE_RING_OPENING_ANGLE / 360f).toLong())
            particle.value = createNewParticle(direction)
        }
    }

    fun playRewardSound() {
        soundPool.play(rewardSoundId, 1f, 1f, 1, 0, 1f)
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

    protected fun createNewParticle(direction: Int): TreeParticle {
        return TreeParticle(
            center = Offset(
                x = SCREEN_CENTER.x - direction * TREE_PARTICLE_DISTANCE_FROM_CENTER,
                y = SCREEN_CENTER.y
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
        particle.success = reward
    }

    private fun mapSpeedToDuration(pxPerSec: Float): Int {
        //TODO: fine tune the mapping function
        val duration = if (pxPerSec <= 750f) {
            TREE_PARTICLE_ANIMATION_MAX_DURATION
        } else {
            val v = 750f / pxPerSec
            TREE_PARTICLE_ANIMATION_MAX_DURATION * v.pow(1.2f)
        }
        return duration.toInt().coerceIn(TREE_PARTICLE_ANIMATION_MIN_DURATION, TREE_PARTICLE_ANIMATION_MAX_DURATION)
    }

    companion object {
        private const val TAG = "TreeViewModel"
    }
}