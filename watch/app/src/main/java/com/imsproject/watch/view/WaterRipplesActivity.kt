package com.imsproject.watch.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val DARK_BACKGROUND_COLOR = 0xFF333842
private const val LIGHT_BLUE_COLOR = 0xFFACC7F6

class WaterRipplesActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WaterRipples()
        }
    }
}

@Composable
fun WaterRipples() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(DARK_BACKGROUND_COLOR)),
        contentAlignment = Alignment.Center
    ) {
        RippleEffect()
    }
}

@Composable
private fun RippleEffect() {
    val ripples = remember { mutableStateListOf<RippleData>() } // Store ripples
    val idCounter = remember { mutableIntStateOf(0) } // Counter for unique ids

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        // Draw all ripples
        ripples.forEach { ripple ->
            Ripple(
                id = ripple.id,
                startSize = ripple.startSize,
                endSize = ripple.endSize,
                duration = ripple.durationMillis,
                onAnimationEnd = {
                    synchronized(ripples) {
                        ripples.removeAt(0)
                    }
                },
            )
        }


        // Button at the center
        Button(
            modifier = Modifier.size(80.dp),
            onClick = {
                val id = idCounter.intValue++
                // Add a new ripple to the list
                ripples.add(
                    RippleData(
                        id = id,
                        startSize = 80f,
                        endSize = 200f,
                        durationMillis = 2000
                    )
                )
            },
        ) {
            // Optional: Add content inside the button if needed
        }
    }
}

@Composable
private fun Ripple(
    id : Int,
    startSize: Float,
    endSize: Float,
    duration: Int,
    onAnimationEnd: () -> Unit
) {
    var currentSize by remember { mutableFloatStateOf(startSize) }
    var alpha by remember { mutableFloatStateOf(1f) }

//    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        launch {
            val sizeAnimStep = (endSize - startSize) / (duration / 16f)
            val alphaAnimStep = 1f / (duration / 16f)

            while (currentSize < endSize) {
                currentSize += sizeAnimStep
                alpha = (alpha-alphaAnimStep).coerceAtLeast(0f)
                delay(16)
            }
            onAnimationEnd()
            println("Ripple $id animation end")
        }
    }

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        drawCircle(
            color = Color(LIGHT_BLUE_COLOR).copy(alpha = alpha),
            radius = currentSize,
            style = Stroke(width = 4.dp.toPx())
        )
    }
}

data class RippleData(
    val id: Int,
    val startSize: Float,
    val endSize: Float,
    val durationMillis: Int
)

@Preview(device = "id:wearos_large_round", apiLevel = 34)
@Composable
fun PreviewWaterRipples() {
    WaterRipples()
}


