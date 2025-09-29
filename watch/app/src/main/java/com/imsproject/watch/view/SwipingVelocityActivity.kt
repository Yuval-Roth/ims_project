package com.imsproject.watch.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.imsproject.watch.utils.FlingTracker
import kotlin.math.hypot
import kotlin.math.min

class SwipingVelocityActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SwipeSpeedScreen() }
    }
}

@Composable
fun SwipeSpeedScreen() {
    var speedPxPerSec by remember { mutableStateOf(0f) }
    var dirX by remember { mutableStateOf(0f) }   // normalized direction x in [-1, 1]
    var dirY by remember { mutableStateOf(0f) }   // normalized direction y in [-1, 1]
    val tracker = remember { FlingTracker() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F4F5))
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
                        val (nx, ny, v) = tracker.endFling() // expects normalized x,y and speed
                        dirX = nx
                        dirY = ny
                        speedPxPerSec = v
                    },
                    onDragCancel = { }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Arrow layer
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val vec = Offset(dirX, dirY)
            val len = hypot(vec.x, vec.y)

            // Visual tuning
            val arrowLength = min(size.minDimension * 0.28f, 180.dp.toPx())
            val shaftWidth = 4.dp.toPx()
            val headLength = 16.dp.toPx()
            val headWidth = 12.dp.toPx()
            val color = Color(0xFF4CAF50)

            if (len < 1e-3f) {
                // No direction → draw a small dot at center
                drawCircle(color = color, radius = 6.dp.toPx(), center = center)
            } else {
                val dir = Offset(vec.x / len, vec.y / len) // unit vector
                val end = center + Offset(dir.x * arrowLength, dir.y * arrowLength)

                // Shaft
                drawLine(
                    color = color,
                    start = center,
                    end = end,
                    strokeWidth = shaftWidth
                )

                // Arrowhead (triangle)
                val baseCenter = end - Offset(dir.x * headLength, dir.y * headLength)
                val perp = Offset(-dir.y, dir.x) // rotate 90°
                val left = baseCenter + Offset(perp.x * (headWidth / 2f), perp.y * (headWidth / 2f))
                val right = baseCenter - Offset(perp.x * (headWidth / 2f), perp.y * (headWidth / 2f))

                val head = Path().apply {
                    moveTo(end.x, end.y)      // tip
                    lineTo(left.x, left.y)    // left base
                    lineTo(right.x, right.y)  // right base
                    close()
                }
                drawPath(head, color = color)
            }
        }

        // Readout
        val speedDpPerSec = with(LocalDensity.current) { speedPxPerSec / density }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = buildString {
                    append(String.format("v: %.0f px/s (%.0f dp/s)\n", speedPxPerSec, speedDpPerSec))
                    append(String.format("dir: (%.2f, %.2f)", dirX, dirY))
                },
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 24.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}
