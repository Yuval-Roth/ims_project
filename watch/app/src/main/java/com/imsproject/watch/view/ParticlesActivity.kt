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
                .background(color = Color.White),
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

            // visual parameters for the mechanical wheel
            val ringRadius = SCREEN_RADIUS * 0.5f
            val ringThickness = SCREEN_RADIUS * 0.08f
            val openingAngle = 40f
            val sweepAngle = 360f - openingAngle
            val teethCount = 12
            val toothDepth = ringThickness * 1.25f
            val toothWidthDegrees = sweepAngle / teethCount * 0.7f // spacing
            val startAngle = -90f + openingAngle / 2
            val toothAngleOffset = 4f // visual tuning, change this as needed to align the teeth to the opening
            val toothWidthRads = Math.toRadians(toothWidthDegrees.toDouble())

            // stationary particles on both sides of the screen
            val particleRadius = (SCREEN_RADIUS * 0.02f)
            val particleRandomOffsetX = (SCREEN_RADIUS * 0.08f)
            val particleRandomOffsetY = (SCREEN_RADIUS * 0.08f)
            val offsetRangeX = -particleRandomOffsetX..particleRandomOffsetX
            val offsetRangeY = -particleRandomOffsetY..particleRandomOffsetY
            val particleDistanceFromCenter = SCREEN_RADIUS * 0.88f
            val cageStrokeWidth = 4f

            // clump of particles on each side in random arrangement
            val particlesPerClump = 30
            val leftClumpOffsets = remember {
                List(particlesPerClump) {
                    Offset(
                        x = -particleDistanceFromCenter + offsetRangeX.random(),
                        y = offsetRangeY.random()
                    )
                }
            }
            val rightClumpOffsets = remember {
                List(particlesPerClump) {
                    Offset(
                        x = particleDistanceFromCenter + offsetRangeX.random(),
                        y = offsetRangeY.random()
                    )
                }
            }

            Canvas(modifier = Modifier.fillMaxSize()) {

                // draw cages for the particles
                // left side
                drawLine(
                    color = Color.Black,
                    start = Offset(0f , SCREEN_CENTER.y - SCREEN_RADIUS * 0.12f),
                    end = Offset(SCREEN_CENTER.x - SCREEN_RADIUS * 0.76f , SCREEN_CENTER.y - SCREEN_RADIUS * 0.12f),
                    strokeWidth = cageStrokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color.Black,
                    start = Offset(0f, SCREEN_CENTER.y + SCREEN_RADIUS * 0.12f),
                    end = Offset(SCREEN_CENTER.x - SCREEN_RADIUS * 0.76f , SCREEN_CENTER.y + SCREEN_RADIUS * 0.12f),
                    strokeWidth = cageStrokeWidth,
                    cap = StrokeCap.Round
                )
                // right side
                drawLine(
                    color = Color.Black,
                    start = Offset(SCREEN_CENTER.x + SCREEN_RADIUS * 0.76f , SCREEN_CENTER.y - SCREEN_RADIUS * 0.12f),
                    end = Offset(size.width , SCREEN_CENTER.y - SCREEN_RADIUS * 0.12f),
                    strokeWidth = cageStrokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color.Black,
                    start = Offset(SCREEN_CENTER.x + SCREEN_RADIUS * 0.76f , SCREEN_CENTER.y + SCREEN_RADIUS * 0.12f),
                    end = Offset(size.width , SCREEN_CENTER.y + SCREEN_RADIUS * 0.12f),
                    strokeWidth = cageStrokeWidth,
                    cap = StrokeCap.Round
                )

                // center expanding circle
                drawCircle(
                    color = Color(0xFF9459EE),
                    radius = SCREEN_RADIUS * 0.075f,
                    center = SCREEN_CENTER
                )

                // mechanical wheel
                rotate(angle) {
                    // ring
                    drawArc(
                        color = Color.Black,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(SCREEN_CENTER.x - ringRadius, SCREEN_CENTER.y - ringRadius),
                        size = Size(ringRadius * 2, ringRadius * 2),
                        style = Stroke(width = ringThickness)
                    )
                    // teeth
                    for (i in 0 until teethCount) {
                        val toothAngle = startAngle + toothAngleOffset + (i * (sweepAngle / teethCount))
                        val rad = Math.toRadians(toothAngle.toDouble())
                        val toothWidthOffset = rad + toothWidthRads
                        val innerRadius = ringRadius + ringThickness / 2
                        val outerRadius = innerRadius + toothDepth

                        // trapezoid corners
                        val p1 = Offset(
                            x = SCREEN_CENTER.x + innerRadius * cos(rad).toFloat(),
                            y = SCREEN_CENTER.y + innerRadius * sin(rad).toFloat()
                        )
                        val radp2 = rad + 0.08f // trapezoid narrower at outer edge
                        val p2 = Offset(
                            x = SCREEN_CENTER.x + outerRadius * cos(radp2).toFloat(),
                            y = SCREEN_CENTER.y + outerRadius * sin(radp2).toFloat()
                        )
                        val radp3 = toothWidthOffset - 0.08f  // trapezoid narrower at outer edge
                        val p3 = Offset(
                            x = SCREEN_CENTER.x + outerRadius * cos(radp3).toFloat(),
                            y = SCREEN_CENTER.y + outerRadius * sin(radp3).toFloat()
                        )
                        val p4 = Offset(
                            x = SCREEN_CENTER.x + innerRadius * cos(toothWidthOffset).toFloat(),
                            y = SCREEN_CENTER.y + innerRadius * sin(toothWidthOffset).toFloat()
                        )
                        val toothPath = Path().apply {
                            moveTo(p1.x, p1.y)
                            lineTo(p2.x, p2.y)
                            lineTo(p3.x, p3.y)
                            lineTo(p4.x, p4.y)
                            close()
                        }
                        drawPath(
                            path = toothPath,
                            color = Color.Black
                        )
                    }
                }

                leftClumpOffsets.forEach {
                    drawCircle(
                        color = Color(0xFFFF4141),
                        radius = particleRadius.toFloat(),
                        center = Offset(SCREEN_CENTER.x + it.x, SCREEN_CENTER.y + it.y)
                    )
                }
                rightClumpOffsets.forEach {
                    drawCircle(
                        color = Color(0xFF3B82F6),
                        radius = particleRadius.toFloat(),
                        center = Offset(SCREEN_CENTER.x + it.x, SCREEN_CENTER.y + it.y)
                    )
                }
            }
        }
    }
}