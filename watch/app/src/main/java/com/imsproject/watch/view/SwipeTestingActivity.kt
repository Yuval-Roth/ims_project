package com.imsproject.watch.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.hypot
import kotlinx.coroutines.launch

class SwipeTestingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                SwipeFillScreen(
                    fillColor = Color(0xFF4CAF50),
                    minDurationMs = 600,
                    maxDurationMs = 1500
                )
            }
        }
    }
}

private enum class FillState { ACTIVE, IDLE }

@Composable
fun SwipeFillScreen(
    fillColor: Color,
    minDurationMs: Int,
    maxDurationMs: Int
) {
    val scope = rememberCoroutineScope()
    val tracker = remember { VelocityTracker() }

    var dirVec by remember { mutableStateOf(Offset(1f, 0f)) } // any-angle direction (unit vector)
    var state by remember { mutableStateOf(FillState.IDLE) }
    val progress = remember { Animatable(0f) } // 0..1

    fun mapSpeedToDuration(speedPxPerSec: Float): Int {
        val s = speedPxPerSec.coerceAtLeast(1f)     // avoid div by 0
        val raw = (220000f / s) * 5.0f              // inverse map: faster → shorter
        return raw.coerceIn(minDurationMs.toFloat(), maxDurationMs.toFloat()).toInt()
    }

    fun normalize(v: Offset): Offset {
        val len = hypot(v.x, v.y)
        return if (len <= 1e-3f) Offset(1f, 0f) else Offset(v.x / len, v.y / len)
    }

    suspend fun startFill(directionVec: Offset, speed: Float) {
        dirVec = normalize(directionVec)
        state = FillState.ACTIVE
        progress.stop()
        progress.snapTo(0f)
        val duration = mapSpeedToDuration(speed)
        progress.animateTo(1f, tween(durationMillis = duration*4, easing = EaseOut))
        // optional reset:
        // state = FillState.IDLE
        // progress.snapTo(0f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .pointerInput(Unit) {
                awaitEachGesture {
                    // start tracking when finger goes down
                    tracker.resetTracking()
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val pointerId = down.id
                    tracker.addPosition(down.uptimeMillis, down.position)

                    while (true) {
                        val event = awaitPointerEvent()
                        val change: PointerInputChange =
                            event.changes.find { it.id == pointerId } ?: break

                        // feed tracker with move samples
                        tracker.addPosition(change.uptimeMillis, change.position)

                        if (change.changedToUpIgnoreConsumed()) {
                            val v = tracker.calculateVelocity()
                            val speed = hypot(v.x, v.y)
                            scope.launch { startFill(Offset(v.x, v.y), speed) }
                            change.consume()
                            break
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Swipe/drag, then lift.\nVelocity at lift controls fill speed & direction (360°).",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp),
            textAlign = TextAlign.Center
        )

        // Any-angle wipe drawing using a half-plane clip
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (state == FillState.IDLE) return@Canvas

            val w = size.width
            val h = size.height
            val d = normalize(dirVec)

            fun proj(p: Offset): Float = p.x * d.x + p.y * d.y
            val rect = listOf(
                Offset(0f, 0f),
                Offset(w, 0f),
                Offset(w, h),
                Offset(0f, h)
            )
            val sMin = rect.minOf { proj(it) }
            val sMax = rect.maxOf { proj(it) }
            val c = sMin + progress.value * (sMax - sMin)

            fun clipByHalfPlane(points: List<Offset>): List<Offset> {
                if (points.isEmpty()) return emptyList()
                val out = mutableListOf<Offset>()
                var prev = points.last()
                var prevIn = proj(prev) <= c

                fun intersect(a: Offset, b: Offset): Offset? {
                    val ab = Offset(b.x - a.x, b.y - a.y)
                    val denom = ab.x * d.x + ab.y * d.y
                    if (kotlin.math.abs(denom) < 1e-6f) return null
                    val t = ((c - (a.x * d.x + a.y * d.y)) / denom).coerceIn(0f, 1f)
                    return Offset(a.x + ab.x * t, a.y + ab.y * t)
                }

                for (curr in points) {
                    val currIn = proj(curr) <= c
                    if (prevIn && currIn) {
                        out.add(curr)
                    } else if (prevIn && !currIn) {
                        intersect(prev, curr)?.let { out.add(it) }
                    } else if (!prevIn && currIn) {
                        intersect(prev, curr)?.let { out.add(it) }
                        out.add(curr)
                    }
                    prev = curr
                    prevIn = currIn
                }
                return out
            }

            val poly = clipByHalfPlane(rect)
            if (poly.size >= 3) {
                val path = Path().apply {
                    moveTo(poly[0].x, poly[0].y)
                    for (i in 1 until poly.size) lineTo(poly[i].x, poly[i].y)
                    close()
                }
                drawPath(path = path, color = fillColor)
            }
        }
    }
}
