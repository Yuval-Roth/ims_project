package com.imsproject.watch.view

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.imsproject.watch.SCREEN_CENTER
import com.imsproject.watch.SCREEN_RADIUS
import com.imsproject.watch.utils.FlingTracker
import com.imsproject.watch.utils.cartesianToPolar
import com.imsproject.watch.viewmodel.PacmanViewModel
import com.imsproject.watch.viewmodel.GameViewModel
import com.imsproject.watch.viewmodel.PacmanViewModel.ParticleState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.lastOrNull
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
        val density =  LocalDensity.current.density
        val tracker = remember { FlingTracker() }
        val scope = rememberCoroutineScope()
        val myParticle by viewModel.myParticle.collectAsState()
        val otherParticle by viewModel.otherParticle.collectAsState()
        val infiniteTransition = rememberInfiniteTransition(label = "rotation")
        val angle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "angle"
        )
        val currentOpeningAngle = angle - 90f
        val pacmanMouthOpeningAnimation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 66f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pacmanMouthOpeningAnimation"
        )

        // visual parameters for the mechanical wheel
        val ringRadius = SCREEN_RADIUS * 0.15f
        val sweepAngle = 360f
        val startAngle = -90f

        // stationary particle on both sides of the screen
        val particleRadius = (SCREEN_RADIUS * 0.02f)
        val particleDistanceFromCenter = SCREEN_RADIUS * 0.88f
        val cageStrokeWidth = 4f
        val cageColor = Color(0xFF0000FF)
        val particleColor = Color(0xFFF3D3C3)

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
                                    targetValue = particleRadius * 2,
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
                                val targetX = SCREEN_CENTER.x - particleRadius
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
                                    viewModel.fling(dpPecSec, myDirection)
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
                    color = cageColor,
                    start = Offset(0f , SCREEN_CENTER.y - SCREEN_RADIUS * 0.12f),
                    end = Offset(SCREEN_CENTER.x - SCREEN_RADIUS * 0.76f , SCREEN_CENTER.y - SCREEN_RADIUS * 0.12f),
                    strokeWidth = cageStrokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = cageColor,
                    start = Offset(SCREEN_CENTER.x - SCREEN_RADIUS * 0.76f, SCREEN_CENTER.y - SCREEN_RADIUS * 0.12f),
                    end = Offset(SCREEN_CENTER.x - SCREEN_RADIUS * 0.76f , SCREEN_CENTER.y - SCREEN_RADIUS * 0.175f),
                    strokeWidth = cageStrokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = cageColor,
                    start = Offset(0f, SCREEN_CENTER.y + SCREEN_RADIUS * 0.12f),
                    end = Offset(SCREEN_CENTER.x - SCREEN_RADIUS * 0.76f , SCREEN_CENTER.y + SCREEN_RADIUS * 0.12f),
                    strokeWidth = cageStrokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = cageColor,
                    start = Offset(SCREEN_CENTER.x - SCREEN_RADIUS * 0.76f , SCREEN_CENTER.y + SCREEN_RADIUS * 0.12f),
                    end = Offset(SCREEN_CENTER.x - SCREEN_RADIUS * 0.76f , SCREEN_CENTER.y + SCREEN_RADIUS * 0.175f),
                    strokeWidth = cageStrokeWidth,
                    cap = StrokeCap.Round
                )
                // right side
                drawLine(
                    color = cageColor,
                    start = Offset(SCREEN_CENTER.x + SCREEN_RADIUS * 0.76f , SCREEN_CENTER.y - SCREEN_RADIUS * 0.12f),
                    end = Offset(size.width , SCREEN_CENTER.y - SCREEN_RADIUS * 0.12f),
                    strokeWidth = cageStrokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = cageColor,
                    start = Offset(SCREEN_CENTER.x + SCREEN_RADIUS * 0.76f , SCREEN_CENTER.y - SCREEN_RADIUS * 0.12f),
                    end = Offset(SCREEN_CENTER.x + SCREEN_RADIUS * 0.76f , SCREEN_CENTER.y - SCREEN_RADIUS * 0.175f),
                    strokeWidth = cageStrokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = cageColor,
                    start = Offset(SCREEN_CENTER.x + SCREEN_RADIUS * 0.76f , SCREEN_CENTER.y + SCREEN_RADIUS * 0.12f),
                    end = Offset(size.width , SCREEN_CENTER.y + SCREEN_RADIUS * 0.12f),
                    strokeWidth = cageStrokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = cageColor,
                    start = Offset(SCREEN_CENTER.x + SCREEN_RADIUS * 0.76f , SCREEN_CENTER.y + SCREEN_RADIUS * 0.12f),
                    end = Offset(SCREEN_CENTER.x + SCREEN_RADIUS * 0.76f , SCREEN_CENTER.y + SCREEN_RADIUS * 0.175f),
                    strokeWidth = cageStrokeWidth,
                    cap = StrokeCap.Round
                )

                // draw particles
                for (particle in listOfNotNull(myParticle, otherParticle)) {
                    drawRoundRect(
                        color = particleColor,
                        topLeft = particle.topLeft,
                        size = particle.size,
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                }

                // rotating pacman
                rotate(angle) {
                    // pacman body
                    drawArc(
                        color = Color.Yellow,
                        startAngle = startAngle + pacmanMouthOpeningAnimation / 2f,
                        sweepAngle = sweepAngle - pacmanMouthOpeningAnimation,
                        useCenter = true,
                        topLeft = Offset(SCREEN_CENTER.x - ringRadius, SCREEN_CENTER.y - ringRadius),
                        size = Size(ringRadius * 2, ringRadius * 2),
                    )
                    // pacman eye
                    val eyeOffsetFromCenter = ringRadius * 0.6f
                    drawCircle(
                        color = Color.White,
                        radius = ringRadius * 0.2f,
                        center = Offset(
                            x = SCREEN_CENTER.x + eyeOffsetFromCenter * cos(Math.toRadians(180.0).toFloat()),
                            y = SCREEN_CENTER.y + eyeOffsetFromCenter * sin(Math.toRadians(180.0).toFloat())
                        )
                    )
                    drawCircle(
                        color = Color.Black,
                        radius = ringRadius * 0.1f,
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