package com.imsproject.watch.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.hypot
import kotlinx.coroutines.launch

class SwipeTestingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                SwipeFillScreen(
                    fillColor = Color(0xFF4CAF50), // change as you like
                    minDurationMs = 500,           // clamp for very fast swipes
                    maxDurationMs = 1500            // clamp for very slow swipes
                )
            }
        }
    }
}

private enum class Direction { LEFT, RIGHT, UP, DOWN, NONE }

@Composable
fun SwipeFillScreen(
    fillColor: Color,
    minDurationMs: Int,
    maxDurationMs: Int
) {
    val scope = rememberCoroutineScope()
    val tracker = remember { VelocityTracker() }

    var direction by remember { mutableStateOf(Direction.NONE) }
    val progress = remember { Animatable(0f) } // 0f..1f

    fun mapSpeedToDuration(speedPxPerSec: Float): Int {
        // guard against 0 and map inversely: bigger speed → shorter duration
        val s = speedPxPerSec.coerceAtLeast(1f)
        // tweak the constant to taste; this gives ~220ms at 5k px/s, ~600ms at 1.8k px/s
        val raw = (220000f / s) * 5.0f
        return raw.coerceIn(minDurationMs.toFloat(), maxDurationMs.toFloat()).toInt()
    }

    suspend fun startFill(dir: Direction, speed: Float) {
        direction = dir
        progress.stop()
        progress.snapTo(0f)
        val duration = mapSpeedToDuration(speed / 2)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = duration, easing = EaseOutCubic)
        )
        // optional: auto-reset after fill completes
        // progress.snapTo(0f); direction = Direction.NONE
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        tracker.resetTracking()
                    },
                    onDrag = { change: PointerInputChange, _ ->
                        tracker.addPosition(change.uptimeMillis, change.position)
                        change.consume()
                    },
                    onDragEnd = {
                        val v = tracker.calculateVelocity()
                        val vx = v.x
                        val vy = v.y
                        val speed = hypot(vx, vy)

                        // Decide direction by dominant axis/sign (no thresholding)
                        val dir = when {
                            abs(vx) >= abs(vy) && vx > 0 -> Direction.RIGHT
                            abs(vx) >= abs(vy) && vx < 0 -> Direction.LEFT
                            abs(vy) >  abs(vx) && vy > 0 -> Direction.DOWN
                            else                         -> Direction.UP
                        }

                        scope.launch { startFill(dir, speed) }
                    },
                    onDragCancel = {
                        direction = Direction.NONE
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Swipe in any direction.\nFaster swipe → faster fill.",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp),
            textAlign = TextAlign.Center
        )

        // Drawing the animated fill
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (direction == Direction.NONE) return@Canvas
            val w = size.width
            val h = size.height
            when (direction) {
                Direction.RIGHT -> {
                    val fillW = w * progress.value
                    drawRect(color = fillColor, size = Size(fillW, h))
                }
                Direction.LEFT -> {
                    val fillW = w * progress.value
                    drawRect(
                        color = fillColor,
                        topLeft = Offset(w - fillW, 0f),
                        size = Size(fillW, h)
                    )
                }
                Direction.DOWN -> {
                    val fillH = h * progress.value
                    drawRect(color = fillColor, size = Size(w, fillH))
                }
                Direction.UP -> {
                    val fillH = h * progress.value
                    drawRect(
                        color = fillColor,
                        topLeft = Offset(0f, h - fillH),
                        size = Size(w, fillH)
                    )
                }
                Direction.NONE -> Unit
            }
        }
    }
}