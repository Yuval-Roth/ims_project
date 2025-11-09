package com.imsproject.watch.view

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.util.fastCoerceAtMost
import com.imsproject.common.gameserver.GameType
import com.imsproject.watch.utils.Angle
import com.imsproject.watch.BLUE_COLOR
import com.imsproject.watch.GRASS_GREEN_COLOR
import com.imsproject.watch.PACMAN_ANGLE_STEP
import com.imsproject.watch.PACMAN_MAX_SIZE
import com.imsproject.watch.PACMAN_MOUTH_OPENING_ANGLE
import com.imsproject.watch.PACMAN_ROTATION_DURATION
import com.imsproject.watch.PACMAN_SHRINK_ANIMATION_DURATION
import com.imsproject.watch.PACMAN_START_ANGLE
import com.imsproject.watch.PACMAN_SWEEP_ANGLE
import com.imsproject.watch.PACMAN_PARTICLE_CAGE_STROKE_WIDTH
import com.imsproject.watch.PACMAN_PARTICLE_COLOR
import com.imsproject.watch.PACMAN_PARTICLE_RADIUS
import com.imsproject.watch.PACMAN_REWARD_SIZE_BONUS
import com.imsproject.watch.SCREEN_CENTER
import com.imsproject.watch.SCREEN_RADIUS
import com.imsproject.watch.utils.FlingTracker
import com.imsproject.watch.utils.cartesianToPolar
import com.imsproject.watch.utils.closestQuantizedAngle
import com.imsproject.watch.utils.quantizeAngles
import com.imsproject.watch.view.contracts.Result
import com.imsproject.watch.viewmodel.PacmanViewModel
import com.imsproject.watch.viewmodel.GameViewModel
import com.imsproject.watch.viewmodel.PacmanViewModel.ParticleState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

class PacmanActivity: GameActivity(GameType.PACMAN) {

    val viewModel: PacmanViewModel by viewModels<PacmanViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.onCreate(viewModel)
        setContent {
            Main()
        }
    }


    @Composable
    override fun Main(){
        val state by viewModel.state.collectAsState()
        when(state){
            GameViewModel.State.PLAYING -> Pacman(viewModel)
            else -> super.Main()
        }
        super.CheckConnection()
    }
}

@Composable
fun Pacman(viewModel: PacmanViewModel) {
    val animatePacman by viewModel.animatePacman.collectAsState()
    val showLeftSide by viewModel.showLeftSide.collectAsState()
    val showRightSide by viewModel.showRightSide.collectAsState()
    val rewardAccumulator = viewModel.rewardAccumulator
    val pacmanAngle = viewModel.pacmanAngle.collectAsState().value
    val density =  LocalDensity.current.density
    val tracker = remember { FlingTracker() }
    val scope = rememberCoroutineScope()
    val myParticle by viewModel.myParticle.collectAsState()
    val otherParticle by viewModel.otherParticle.collectAsState()
    var fedSuccessfully by remember { mutableStateOf(false) }

    val pacmanRadius = (SCREEN_RADIUS * (0.15f + rewardAccumulator.value * PACMAN_REWARD_SIZE_BONUS)).fastCoerceAtMost(PACMAN_MAX_SIZE)

    LaunchedEffect(animatePacman){
        if(!animatePacman) return@LaunchedEffect
        val pacmanAngle = viewModel.pacmanAngle
        // rotate pacman and check for feeding events
        val quantizedAngles = quantizeAngles(PACMAN_ANGLE_STEP)
        val leftThreshold = closestQuantizedAngle(180f + PACMAN_MOUTH_OPENING_ANGLE+PACMAN_ANGLE_STEP, PACMAN_ANGLE_STEP, quantizedAngles)
        val rightThreshold = closestQuantizedAngle(PACMAN_MOUTH_OPENING_ANGLE+PACMAN_ANGLE_STEP, PACMAN_ANGLE_STEP, quantizedAngles)
        while(true){
            val currentGameTime = viewModel.getCurrentGameTime()
            val rotationTimePassed = currentGameTime % PACMAN_ROTATION_DURATION
            val rotationPercentage = rotationTimePassed / PACMAN_ROTATION_DURATION
            val targetAngle = 360f * rotationPercentage
            val quantizedTargetAngle = closestQuantizedAngle(targetAngle,PACMAN_ANGLE_STEP, quantizedAngles)
            pacmanAngle.value = Angle.fromArbitraryAngle(quantizedTargetAngle)

            // check for feeding
            if(quantizedTargetAngle == leftThreshold || quantizedTargetAngle == rightThreshold){
                if(!fedSuccessfully){
                    scope.launch {
                        rewardAccumulator.animateTo(
                            targetValue = (rewardAccumulator.value - 1).coerceAtLeast(0f),
                            animationSpec = tween(
                                durationMillis = PACMAN_SHRINK_ANIMATION_DURATION,
                                easing = LinearEasing
                            )
                        )
                    }
                } else {
                    fedSuccessfully = false
                }
            }
            awaitFrame()
        }
    }

    // animate new particles
    LaunchedEffect(Unit) {
        while(true){
            for (particle in listOfNotNull(myParticle, otherParticle)) {
                when(particle.state){
                    ParticleState.NEW -> {
                        particle.state = ParticleState.STATIONARY
                        val sizeAnimation = Animatable(0f)
                        scope.launch {
                            sizeAnimation.animateTo(
                                targetValue = PACMAN_PARTICLE_RADIUS * 2,
                                animationSpec = tween(
                                    durationMillis = 150,
                                    easing = LinearEasing
                                )
                            ) {
                                particle.size = Size(value, value)
                            }
                        }
                    }
                    ParticleState.STATIONARY -> {
                        if (particle.animationLength > 0) {
                            particle.state = ParticleState.MOVING
                            val targetX = SCREEN_CENTER.x - PACMAN_PARTICLE_RADIUS
                            val startX = particle.topLeft.x
                            val distance = targetX - startX
                            val positionAnimation = Animatable(0f)
                            if(particle.reward){
                                scope.launch(Dispatchers.Default) {
                                    delay(particle.animationLength - 250L)
                                    viewModel.playRewardSound()
                                }
                            }
                            scope.launch {
                                positionAnimation.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(
                                        durationMillis = particle.animationLength,
                                        easing = LinearEasing
                                    )
                                ) {
                                    val newX = startX + distance * value
                                    particle.topLeft = Offset(newX, particle.topLeft.y)
                                }
                                if (particle.reward){
                                    rewardAccumulator.snapTo(rewardAccumulator.targetValue + 1f)
                                    fedSuccessfully = true
                                }
                                // after animation ends, reset particle
                                viewModel.resetParticle(particle.direction)
                            }
                        }
                    }
                    ParticleState.MOVING -> { /* do nothing, animation is already running */ }
                }
            }
            awaitFrame()
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.Black)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        val x = startOffset.x
                        val y = startOffset.y
                        val (distance, _) = cartesianToPolar(x, y)
                        val direction = if (x < SCREEN_CENTER.x) 1 else -1
                        if (viewModel.myDirection == direction && distance > SCREEN_RADIUS * 0.6) {
                            tracker.startFling(x, y)
                        }
                    },
                    onDrag = { change: PointerInputChange, _ ->
                        val position = change.position
                        tracker.setOffset(position.x, position.y)
                    },
                    onDragEnd = {
                        if (tracker.started) {
                            val (nx, ny, speedPxPerSec) = tracker.endFling()
                            if (viewModel.myDirection * nx > 0) { // fling in my direction
                                val dpPecSec = speedPxPerSec / density
                                viewModel.fling(dpPecSec)
                            }
                        }
                    },
                    onDragCancel = { }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {

            // draw cages for the particles
            val WALLS_UPPER_Y = SCREEN_CENTER.y - SCREEN_RADIUS * 0.12f
            val WALLS_LOWER_Y = SCREEN_CENTER.y + SCREEN_RADIUS * 0.12f
            val OPENING_UPPER_Y = SCREEN_CENTER.y - SCREEN_RADIUS * 0.175f
            val OPENING_LOWER_Y = SCREEN_CENTER.y + SCREEN_RADIUS * 0.175f
            // left side
            if(showLeftSide){
                val LEFT_INNER_X = SCREEN_CENTER.x - SCREEN_RADIUS * 0.76f
                drawLine(
                    color = BLUE_COLOR,
                    start = Offset(0f , WALLS_UPPER_Y),
                    end = Offset(LEFT_INNER_X, WALLS_UPPER_Y),
                    strokeWidth = PACMAN_PARTICLE_CAGE_STROKE_WIDTH,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = BLUE_COLOR,
                    start = Offset(LEFT_INNER_X, WALLS_UPPER_Y),
                    end = Offset(LEFT_INNER_X, OPENING_UPPER_Y),
                    strokeWidth = PACMAN_PARTICLE_CAGE_STROKE_WIDTH,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = BLUE_COLOR,
                    start = Offset(0f, WALLS_LOWER_Y),
                    end = Offset(LEFT_INNER_X, WALLS_LOWER_Y),
                    strokeWidth = PACMAN_PARTICLE_CAGE_STROKE_WIDTH,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = BLUE_COLOR,
                    start = Offset(LEFT_INNER_X, WALLS_LOWER_Y),
                    end = Offset(LEFT_INNER_X, OPENING_LOWER_Y),
                    strokeWidth = PACMAN_PARTICLE_CAGE_STROKE_WIDTH,
                    cap = StrokeCap.Round
                )
            }
            // right side
            if(showRightSide){
                val RIGHT_INNER_X = SCREEN_CENTER.x + SCREEN_RADIUS * 0.76f
                drawLine(
                    color = GRASS_GREEN_COLOR,
                    start = Offset(RIGHT_INNER_X, WALLS_UPPER_Y),
                    end = Offset(size.width , WALLS_UPPER_Y),
                    strokeWidth = PACMAN_PARTICLE_CAGE_STROKE_WIDTH,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = GRASS_GREEN_COLOR,
                    start = Offset(RIGHT_INNER_X, WALLS_UPPER_Y),
                    end = Offset(RIGHT_INNER_X, OPENING_UPPER_Y),
                    strokeWidth = PACMAN_PARTICLE_CAGE_STROKE_WIDTH,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = GRASS_GREEN_COLOR,
                    start = Offset(RIGHT_INNER_X, WALLS_LOWER_Y),
                    end = Offset(size.width , WALLS_LOWER_Y),
                    strokeWidth = PACMAN_PARTICLE_CAGE_STROKE_WIDTH,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = GRASS_GREEN_COLOR,
                    start = Offset(RIGHT_INNER_X, WALLS_LOWER_Y),
                    end = Offset(RIGHT_INNER_X, OPENING_LOWER_Y),
                    strokeWidth = PACMAN_PARTICLE_CAGE_STROKE_WIDTH,
                    cap = StrokeCap.Round
                )
            }

            // draw particles
            for (particle in listOfNotNull(myParticle, otherParticle)) {
                drawRoundRect(
                    color = PACMAN_PARTICLE_COLOR,
                    topLeft = particle.topLeft,
                    size = particle.size,
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }

            // rotating pacman
            rotate(pacmanAngle.floatValue + 90f) { // 90f offset to align 0 degrees to the right
                // pacman body
                drawArc(
                    color = Color.Yellow,
                    startAngle = PACMAN_START_ANGLE,
                    sweepAngle = PACMAN_SWEEP_ANGLE,
                    useCenter = true,
                    topLeft = Offset(SCREEN_CENTER.x - pacmanRadius, SCREEN_CENTER.y - pacmanRadius),
                    size = Size(pacmanRadius * 2, pacmanRadius * 2),
                )
                // pacman eye
                val eyeOffsetFromCenter = pacmanRadius * 0.6f
                drawCircle(
                    color = Color.Black,
                    radius = pacmanRadius * 0.1f,
                    center = Offset(
                        x = SCREEN_CENTER.x + eyeOffsetFromCenter * cos(Math.toRadians(180.0).toFloat()),
                        y = SCREEN_CENTER.y + eyeOffsetFromCenter * sin(Math.toRadians(180.0).toFloat())
                    )
                )
            }
        }
    }
}