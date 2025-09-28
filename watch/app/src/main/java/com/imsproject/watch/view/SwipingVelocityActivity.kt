package com.imsproject.watch.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.imsproject.watch.utils.FlingTracker
import kotlin.math.hypot

class SwipingVelocityActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SwipeSpeedScreen() }
    }
}

@Composable
fun SwipeSpeedScreen() {
    var speedPxPerSec by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
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
                    onDrag = { _, offset ->
                        tracker.setOffset(offset.x,offset.y)
                    },
                    onDragEnd = {
                        val (x, y, v) = tracker.endFling()
                        speedPxPerSec = v
                    },
                    onDragCancel = { }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val speedDpPerSec = with(density) { speedPxPerSec / density.density }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = buildString {
                    append(String.format("%.0f px/s", speedPxPerSec))
                    append("  •  \n")
                    append(String.format("%.0f dp/s", speedDpPerSec))
                },
                fontSize = 16.sp,
                modifier = Modifier
                    .padding(horizontal = 24.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}
