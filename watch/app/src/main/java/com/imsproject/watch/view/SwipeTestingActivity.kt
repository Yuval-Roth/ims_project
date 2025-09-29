package com.imsproject.watch.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutExpo
import androidx.compose.animation.core.EaseInOutQuad
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.imsproject.watch.utils.FlingTracker
import com.imsproject.watch.utils.OceanWaveEasing
import com.imsproject.watch.utils.SampledEasing
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.math.hypot

class SwipeTestingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                SwipeFillScreen(
                    fillColor = Color(0xFF4CAF50),
                    minDurationMs = 1500,
                    maxDurationMs = 5000
                )
            }
        }
    }
}

private enum class FillState { ACTIVE, IDLE }

// 100 samples from [0,1] with fast start, slow end
//private val easingSamples = listOf(
//    0.0f, 0.058952f, 0.114438f, 0.16666f, 0.215812f,
//    0.262073f, 0.305614f, 0.346594f, 0.385164f, 0.421466f,
//    0.455634f, 0.487792f, 0.518059f, 0.546546f, 0.573358f,
//    0.598593f, 0.622345f, 0.644699f, 0.665739f, 0.685542f,
//    0.70418f, 0.721722f, 0.738233f, 0.753772f, 0.768398f,
//    0.782164f, 0.79512f, 0.807314f, 0.818792f, 0.829594f,
//    0.839761f, 0.84933f, 0.858337f, 0.866813f, 0.874792f,
//    0.882301f, 0.889368f, 0.89602f, 0.902281f, 0.908174f,
//    0.91372f, 0.91894f, 0.923853f, 0.928477f, 0.932829f,
//    0.936925f, 0.94078f, 0.944409f, 0.947824f, 0.951039f,
//    0.954064f, 0.956911f, 0.959591f, 0.962114f, 0.964488f,
//    0.966722f, 0.968825f, 0.970805f, 0.972668f, 0.974421f,
//    0.976071f, 0.977625f, 0.979087f, 0.980463f, 0.981758f,
//    0.982977f, 0.984124f, 0.985204f, 0.98622f, 0.987176f,
//    0.988076f, 0.988924f, 0.989721f, 0.990472f, 0.991178f,
//    0.991843f, 0.992469f, 0.993058f, 0.993612f, 0.994134f,
//    0.994625f, 0.995087f, 0.995522f, 0.995932f, 0.996317f,
//    0.99668f, 0.997021f, 0.997343f, 0.997645f, 0.99793f,
//    0.998197f, 0.99845f, 0.998687f, 0.99891f, 0.99912f,
//    0.999318f, 0.999505f, 0.99968f, 0.999845f, 1.0f
//)

// 100 samples from [0,1] — gentler start than before (energy = 1.5)
private val easingSamples = List(100) { i ->
    if (i == 0) return@List 0f
    if (i == 99) return@List 1f
    val t = i / 99f
    val early = 1f - exp(-2.85f * t)
    early
}

@Composable
fun SwipeFillScreen(
    fillColor: Color,
    minDurationMs: Int,
    maxDurationMs: Int
) {
    val density =  LocalDensity.current.density
    val scope = rememberCoroutineScope()
    val tracker = remember { FlingTracker() }
    val easing = remember {
        println(easingSamples.joinToString(","))
        SampledEasing(easingSamples)
    }

    var dirVec by remember { mutableStateOf(Offset(1f, 0f)) } // any-angle direction (unit vector)
    var state by remember { mutableStateOf(FillState.IDLE) }
    val progress = remember { Animatable(0f) } // 0..1

    fun mapSpeedToDuration(dpPerSec: Float): Int {
        val pxPerSec = dpPerSec * density
        val duration = if (pxPerSec <= 1000f) {
            maxDurationMs.toFloat()
        } else {
            val v = 1000f / pxPerSec
            val duration = maxDurationMs * v
            duration.coerceIn(minDurationMs.toFloat(), maxDurationMs.toFloat())
        }
        return duration.toInt()
    }

    fun normalize(v: Offset): Offset {
        val len = hypot(v.x, v.y)
        return if (len <= 1e-3f) Offset(1f, 0f) else Offset(v.x / len, v.y / len)
    }

    suspend fun startFill(directionVec: Offset, dpPerSec: Float) {
        dirVec = directionVec
        state = FillState.ACTIVE
        progress.stop()
        progress.snapTo(0f)
        val duration = mapSpeedToDuration(dpPerSec)
        progress.animateTo(1f, tween(durationMillis = duration, easing = OceanWaveEasing))
        // optional reset:
        // state = FillState.IDLE
        // progress.snapTo(0f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        tracker.startFling(startOffset.x, startOffset.y)
                    },
                    onDrag = { change: PointerInputChange, _ ->
                        val position = change.position
                        tracker.setOffset(position.x, position.y)
                    },
                    onDragEnd = {
                        val (nx, ny, speedPxPerSec) = tracker.endFling()
                        val dpPecSec = speedPxPerSec / density
                        scope.launch {
                            startFill(Offset(nx, ny), dpPecSec)
                        }
                    },
                    onDragCancel = { }
                )
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
