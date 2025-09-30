package com.imsproject.watch.view

import android.os.Bundle
import android.view.WindowManager
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
import com.imsproject.watch.SCREEN_RADIUS
import com.imsproject.watch.initProperties
import com.imsproject.watch.utils.FlingTracker
import com.imsproject.watch.utils.OceanWaveEasing
import com.imsproject.watch.utils.SampledEasing
import com.imsproject.watch.utils.cartesianToPolar
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.pow

class SwipeTestingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val metrics = getSystemService(WindowManager::class.java).currentWindowMetrics
        initProperties(metrics.bounds.width())
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
            val duration = maxDurationMs * v.pow(1.5f)
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
                        val x = startOffset.x
                        val y = startOffset.y
                        val (distance, _) = cartesianToPolar(x,y)
                        if (distance > SCREEN_RADIUS * 0.8)
                            tracker.startFling(x, y)
                    },
                    onDrag = { change: PointerInputChange, _ ->
                        val position = change.position
                        tracker.setOffset(position.x, position.y)
                    },
                    onDragEnd = {
                        if (tracker.started){
                            val (nx, ny, speedPxPerSec) = tracker.endFling()
                            val dpPecSec = speedPxPerSec / density
                            scope.launch {
                                startFill(Offset(nx, ny), dpPecSec)
                            }
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
