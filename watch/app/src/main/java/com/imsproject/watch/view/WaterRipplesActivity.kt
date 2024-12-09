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
private const val BUTTON_SIZE = 80
private const val RIPPLE_MAX_SIZE = 225
private const val ANIMATION_DURATION = 2000

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
    var ripples = remember { mutableStateListOf<Boolean>() } // Store ripples

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {

        // i have actually no fucking clue why this works
        ripples.forEach { Ripple() }

        Button(
            modifier = Modifier.size(80.dp),
            onClick = {
                ripples.add(true) // Again, no clue why this works
            },
        ){
            // No content as of now
        }
    }
}

@Composable
private fun Ripple() {

    val startSize = BUTTON_SIZE.toFloat()
    val endSize = RIPPLE_MAX_SIZE.toFloat()
    val duration = ANIMATION_DURATION

    var currentSize by remember { mutableFloatStateOf(startSize) }
    var alpha by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(Unit) {
        launch {
            val sizeAnimStep = (endSize - startSize) / (duration / 16f)
            val alphaAnimStep = 1f / (duration / 16f)

            while (currentSize < endSize) {
                currentSize += sizeAnimStep
                alpha = (alpha-alphaAnimStep).coerceAtLeast(0f)
                delay(16)
            }
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

@Preview(device = "id:wearos_large_round", apiLevel = 34)
@Composable
fun PreviewWaterRipples() {
    WaterRipples()
}


