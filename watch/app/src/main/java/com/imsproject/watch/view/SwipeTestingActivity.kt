package com.imsproject.watch.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.EaseOutElastic
import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.EaseOutQuint
import androidx.compose.animation.core.EaseOutSine
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
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
                    fillColor = Color(0xFF4CAF50),
                    minDurationMs = 500,
                    maxDurationMs = 4000,
                    dragThresholdDp = 64f,
                    flickVelocityThresholdPxPerSec = 1600f // ← new: tune to taste
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
    maxDurationMs: Int,
    dragThresholdDp: Float,
    flickVelocityThresholdPxPerSec: Float
) {
    val scope = rememberCoroutineScope()
    val tracker = remember { VelocityTracker() }
    val density = LocalDensity.current
    val dragThresholdPx = with(density) { dragThresholdDp.dp.toPx() }

    var direction by remember { mutableStateOf(Direction.NONE) }
    val progress = remember { Animatable(0f) } // 0f..1f

    // track a single trigger per gesture
    var triggeredThisGesture by remember { mutableStateOf(false) }
    var dragStart by remember { mutableStateOf(Offset.Unspecified) }

    fun mapSpeedToDuration(speedPxPerSec: Float): Int {
        val s = speedPxPerSec.coerceAtLeast(1f)
        // inverse map: faster speed → shorter duration
        val raw = (220000f / s) * 5.0f
        return raw.coerceIn(minDurationMs.toFloat(), maxDurationMs.toFloat()).toInt()
    }

    suspend fun startFill(dir: Direction, speed: Float) {
        direction = dir
        progress.stop()
        progress.snapTo(0f)
        val duration = mapSpeedToDuration(speed)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = duration, easing = EaseOut)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .pointerInput(dragThresholdPx, flickVelocityThresholdPxPerSec) {
                detectDragGestures(
                    onDragStart = { start ->
                        tracker.resetTracking()
                        dragStart = start
                        triggeredThisGesture = false
                    },
                    onDrag = { change: PointerInputChange, _ ->
                        tracker.addPosition(change.uptimeMillis, change.position)

                        if (!triggeredThisGesture && dragStart.isSpecified) {
                            val dx = change.position.x - dragStart.x
                            val dy = change.position.y - dragStart.y
                            val dist = hypot(dx, dy)

                            if (dist >= dragThresholdPx) {
                                // Direction from cumulative delta
                                val dir = when {
                                    abs(dx) >= abs(dy) && dx > 0 -> Direction.RIGHT
                                    abs(dx) >= abs(dy) && dx < 0 -> Direction.LEFT
                                    abs(dy) >  abs(dx) && dy > 0 -> Direction.DOWN
                                    else                         -> Direction.UP
                                }

                                val v = tracker.calculateVelocity()
                                val speed = hypot(v.x, v.y)

                                triggeredThisGesture = true
                                scope.launch { startFill(dir, speed) }
                            }
                        }
                        change.consume()
                    },
                    onDragEnd = {
                        // Quick flick support: trigger on high velocity even if distance < threshold
                        if (!triggeredThisGesture) {
                            val v = tracker.calculateVelocity()
                            val vx = v.x
                            val vy = v.y
                            val speed = hypot(vx, vy)

                            if (speed >= flickVelocityThresholdPxPerSec) {
                                val dir = when {
                                    abs(vx) >= abs(vy) && vx > 0 -> Direction.RIGHT
                                    abs(vx) >= abs(vy) && vx < 0 -> Direction.LEFT
                                    abs(vy) >  abs(vx) && vy > 0 -> Direction.DOWN
                                    else                         -> Direction.UP
                                }
                                triggeredThisGesture = true
                                scope.launch { startFill(dir, speed) }
                            }
                        }
                        dragStart = Offset.Unspecified
                        triggeredThisGesture = false
                    },
                    onDragCancel = {
                        dragStart = Offset.Unspecified
                        triggeredThisGesture = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Drag ~${dragThresholdDp.toInt()}dp OR flick.\nFaster motion → faster fill.",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp),
            textAlign = TextAlign.Center
        )

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
