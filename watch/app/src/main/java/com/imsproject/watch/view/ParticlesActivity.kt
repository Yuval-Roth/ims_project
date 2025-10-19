package com.imsproject.watch.view

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.imsproject.common.gameserver.GameType
import com.imsproject.watch.SCREEN_CENTER
import com.imsproject.watch.SCREEN_RADIUS
import com.imsproject.watch.utils.random
import com.imsproject.watch.viewmodel.ParticlesViewModel
import com.imsproject.watch.viewmodel.GameViewModel
import kotlin.math.cos
import kotlin.math.sin

class ParticlesActivity: GameActivity(GameType.PARTICLES) {

    val viewModel: ParticlesViewModel by viewModels<ParticlesViewModel>()

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
            GameViewModel.State.PLAYING -> Particles()
            else -> super.Main()
        }
    }

    @Composable
    fun Particles() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color.Black),
            contentAlignment = Alignment.Center
        ) {
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
            val cageColor = Color(0xFF0000ff)

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

                // draw stationary particles
                // left side
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(
                        x = SCREEN_CENTER.x - particleDistanceFromCenter - particleRadius,
                        y = SCREEN_CENTER.y - particleRadius
                    ),
                    size = Size(particleRadius * 2, particleRadius * 2),
                    cornerRadius = CornerRadius(2f, 2f)
                )
                // right side
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(
                        x = SCREEN_CENTER.x + particleDistanceFromCenter - particleRadius,
                        y = SCREEN_CENTER.y - particleRadius
                    ),
                    size = Size(particleRadius * 2, particleRadius * 2),
                    cornerRadius = CornerRadius(2f, 2f)
                )


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