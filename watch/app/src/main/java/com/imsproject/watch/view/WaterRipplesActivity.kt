package com.imsproject.watch.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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

private const val DARK_BACKGROUND_COLOR = 0xFF333842
private const val LIGHT_BLUE_COLOR = 0xFFACC7F6
private const val BUTTON_SIZE = 80
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
    var isAnimating by remember { mutableStateOf(false) } // Animation state

    // Animation for the ripple size
    val animatedRippleSize by animateFloatAsState(
        targetValue = if (isAnimating) 200f else BUTTON_SIZE.toFloat(),
        animationSpec = spring(stiffness = 30f),
        finishedListener = { isAnimating = false }, label = "size" // Reset animation state
    )

    // Animation for the alpha (fade out)
    val alpha by animateFloatAsState(
        targetValue = if (isAnimating) 0f else 1f,
        animationSpec = spring(stiffness = 15f),
        label = "alpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        // Draw the ripple as a fading circle
        androidx.compose.foundation.Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            if (isAnimating) {
                drawCircle(
                    color = Color(LIGHT_BLUE_COLOR).copy(alpha = alpha),
                    radius = animatedRippleSize,
                    style = Stroke(width = 6.dp.toPx())
                )
            }
        }

        // Button at the center
        Button(
            modifier = Modifier.size(BUTTON_SIZE.dp),
            onClick = {
                isAnimating = true // Trigger the animation
            },
        ) {
            // Optional: Add content inside the button if needed
        }
    }
}

@Preview(device = "id:wearos_large_round", apiLevel = 34)
@Composable
fun PreviewWaterRipples() {
    WaterRipples()
}
