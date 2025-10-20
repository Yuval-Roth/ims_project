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
import com.imsproject.common.gameserver.GameType
import com.imsproject.common.utils.Angle
import com.imsproject.watch.ANGLE_ROTATION_DURATION
import com.imsproject.watch.PACMAN_MOUTH_OPENING_ANGLE
import com.imsproject.watch.PACMAN_SHRINK_ANIMATION_DURATION
import com.imsproject.watch.PACMAN_START_ANGLE
import com.imsproject.watch.PACMAN_SWEEP_ANGLE
import com.imsproject.watch.PARTICLE_CAGE_COLOR
import com.imsproject.watch.PARTICLE_CAGE_STROKE_WIDTH
import com.imsproject.watch.PARTICLE_COLOR
import com.imsproject.watch.PARTICLE_RADIUS
import com.imsproject.watch.SCREEN_CENTER
import com.imsproject.watch.SCREEN_RADIUS
import com.imsproject.watch.utils.FlingTracker
import com.imsproject.watch.utils.cartesianToPolar
import com.imsproject.watch.viewmodel.PacmanViewModel
import com.imsproject.watch.viewmodel.GameViewModel
import com.imsproject.watch.viewmodel.PacmanViewModel.ParticleState
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
            GameViewModel.State.PLAYING -> Pacman()
            else -> super.Main()
        }
    }

    @Composable
    fun Pacman() {
        val rewardAccumulator = remember { Animatable(0f)}
        val density =  LocalDensity.current.density
        val tracker = remember { FlingTracker() }
        val scope = rememberCoroutineScope()
        val myParticle by viewModel.myParticle.collectAsState()
        val otherParticle by viewModel.otherParticle.collectAsState()
        var angle by viewModel.angle
        val angleStep = 360f / (ANGLE_ROTATION_DURATION / 16f)
        var fedSuccessfully by remember { mutableStateOf(false) }

        val pacmanRadius = SCREEN_RADIUS * ((0.15f + rewardAccumulator.value * 0.01f).coerceAtMost(0.7f))

        LaunchedEffect(Unit){
            // build angle thresholds
            // we do this iteratively because of float precision issues
            var leftThresholdAccumulator = Angle(0f)
            while(leftThresholdAccumulator.floatValue >= 0){
                // get to the left side of the circle
                leftThresholdAccumulator = leftThresholdAccumulator + angleStep
            }
            while(leftThresholdAccumulator.floatValue.toInt() <= -180 + PACMAN_MOUTH_OPENING_ANGLE){
                leftThresholdAccumulator = leftThresholdAccumulator + angleStep
            }
            var rightThresholdAccumulator = Angle(0f)
            while(rightThresholdAccumulator.floatValue.toInt() <= PACMAN_MOUTH_OPENING_ANGLE){
                rightThresholdAccumulator = rightThresholdAccumulator + angleStep
            }
            val leftThreshold = leftThresholdAccumulator.floatValue.toInt()
            val rightThreshold = rightThresholdAccumulator.floatValue.toInt()

            // rotate pacman and check for feeding events
            while(true){
                angle = angle + angleStep
                if(angle.floatValue.toInt() == 0){
                    // reset angle to handle float precision issues
                    angle = Angle(0f)
                }
                val flooredAngle = angle.floatValue.toInt()
                if(flooredAngle == leftThreshold || flooredAngle == rightThreshold){
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

                delay(16L)
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
                                    targetValue = PARTICLE_RADIUS * 2,
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
                                val targetX = SCREEN_CENTER.x - PARTICLE_RADIUS
                                val startX = particle.topLeft.x
                                val distance = targetX - startX
                                val positionAnimation = Animatable(0f)
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
                delay(16L)
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
                                val myDirection = viewModel.myDirection
                                if (myDirection * nx > 0) { // fling in my direction
                                    val dpPecSec = speedPxPerSec / density
                                    viewModel.fling(dpPecSec, angle)
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
                // left side
                drawLine(
                    color = PARTICLE_CAGE_COLOR,
                    start = Offset(0f , SCREEN_CENTER.y - SCREEN_RADIUS * 0.12f),
                    end = Offset(SCREEN_CENTER.x - SCREEN_RADIUS * 0.76f , SCREEN_CENTER.y - SCREEN_RADIUS * 0.12f),
                    strokeWidth = PARTICLE_CAGE_STROKE_WIDTH,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = PARTICLE_CAGE_COLOR,
                    start = Offset(SCREEN_CENTER.x - SCREEN_RADIUS * 0.76f, SCREEN_CENTER.y - SCREEN_RADIUS * 0.12f),
                    end = Offset(SCREEN_CENTER.x - SCREEN_RADIUS * 0.76f , SCREEN_CENTER.y - SCREEN_RADIUS * 0.175f),
                    strokeWidth = PARTICLE_CAGE_STROKE_WIDTH,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = PARTICLE_CAGE_COLOR,
                    start = Offset(0f, SCREEN_CENTER.y + SCREEN_RADIUS * 0.12f),
                    end = Offset(SCREEN_CENTER.x - SCREEN_RADIUS * 0.76f , SCREEN_CENTER.y + SCREEN_RADIUS * 0.12f),
                    strokeWidth = PARTICLE_CAGE_STROKE_WIDTH,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = PARTICLE_CAGE_COLOR,
                    start = Offset(SCREEN_CENTER.x - SCREEN_RADIUS * 0.76f , SCREEN_CENTER.y + SCREEN_RADIUS * 0.12f),
                    end = Offset(SCREEN_CENTER.x - SCREEN_RADIUS * 0.76f , SCREEN_CENTER.y + SCREEN_RADIUS * 0.175f),
                    strokeWidth = PARTICLE_CAGE_STROKE_WIDTH,
                    cap = StrokeCap.Round
                )
                // right side
                drawLine(
                    color = PARTICLE_CAGE_COLOR,
                    start = Offset(SCREEN_CENTER.x + SCREEN_RADIUS * 0.76f , SCREEN_CENTER.y - SCREEN_RADIUS * 0.12f),
                    end = Offset(size.width , SCREEN_CENTER.y - SCREEN_RADIUS * 0.12f),
                    strokeWidth = PARTICLE_CAGE_STROKE_WIDTH,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = PARTICLE_CAGE_COLOR,
                    start = Offset(SCREEN_CENTER.x + SCREEN_RADIUS * 0.76f , SCREEN_CENTER.y - SCREEN_RADIUS * 0.12f),
                    end = Offset(SCREEN_CENTER.x + SCREEN_RADIUS * 0.76f , SCREEN_CENTER.y - SCREEN_RADIUS * 0.175f),
                    strokeWidth = PARTICLE_CAGE_STROKE_WIDTH,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = PARTICLE_CAGE_COLOR,
                    start = Offset(SCREEN_CENTER.x + SCREEN_RADIUS * 0.76f , SCREEN_CENTER.y + SCREEN_RADIUS * 0.12f),
                    end = Offset(size.width , SCREEN_CENTER.y + SCREEN_RADIUS * 0.12f),
                    strokeWidth = PARTICLE_CAGE_STROKE_WIDTH,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = PARTICLE_CAGE_COLOR,
                    start = Offset(SCREEN_CENTER.x + SCREEN_RADIUS * 0.76f , SCREEN_CENTER.y + SCREEN_RADIUS * 0.12f),
                    end = Offset(SCREEN_CENTER.x + SCREEN_RADIUS * 0.76f , SCREEN_CENTER.y + SCREEN_RADIUS * 0.175f),
                    strokeWidth = PARTICLE_CAGE_STROKE_WIDTH,
                    cap = StrokeCap.Round
                )

                // draw particles
                for (particle in listOfNotNull(myParticle, otherParticle)) {
                    drawRoundRect(
                        color = PARTICLE_COLOR,
                        topLeft = particle.topLeft,
                        size = particle.size,
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                }

                // rotating pacman
                rotate(angle.floatValue + 90f) {
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
}