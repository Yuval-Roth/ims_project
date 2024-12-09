package com.imsproject.watch.view

import android.R.attr.duration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import com.imsproject.watch.viewmodel.WaterRipplesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val DARK_BACKGROUND_COLOR = 0xFF333842
private const val LIGHT_BLUE_COLOR = 0xFFACC7F6
private const val BUTTON_SIZE = 80
private const val RIPPLE_MAX_SIZE = 225
private const val ANIMATION_DURATION = 2000

class WaterRipplesActivity : ComponentActivity() {

    val viewModel : WaterRipplesViewModel by viewModels<WaterRipplesViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WaterRipples(viewModel)
        }
    }
}

@Composable
fun WaterRipples(viewModel: WaterRipplesViewModel) {

    var ripples = viewModel.ripples
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(DARK_BACKGROUND_COLOR)),
        contentAlignment = Alignment.Center
    ) {

        Button(
            modifier = Modifier.size(80.dp),
            onClick = {
                viewModel.addRipple()
            },
        ){
            // no content
        }

        // ================================================= |
        // =============== Ripple Effect =================== |
        // ================================================= |

        val startSize = BUTTON_SIZE.toFloat()
        val endSize = RIPPLE_MAX_SIZE.toFloat()
        val duration = ANIMATION_DURATION

        val sizeAnimStep = (endSize - startSize) / (duration / 16f)
        val alphaAnimStep = 1f / (duration / 16f)

        LaunchedEffect(ripples.size.toString()) {
            for (ripple in ripples) {
                launch {
                    while (ripple.size.floatValue < endSize) {
                        ripple.size.floatValue += sizeAnimStep
                        ripple.alpha.floatValue = (ripple.alpha.floatValue-alphaAnimStep).coerceAtLeast(0f)
                        delay(16)
                    }
                }
            }
        }

        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            for(ripple in ripples){
                val color = Color(ripple.color.longValue)
                val size = ripple.size.floatValue
                val alpha = ripple.alpha.floatValue

                drawCircle(
                    color = color.copy(alpha = alpha),
                    radius = size,
                    style = Stroke(width = 4.dp.toPx())
                )
            }
        }

    }
}

private const val VIVID_ORANGE_COLOR = 0xFFFF5722

@Composable
private fun Ripple(
    color: Color
) {

    val startSize = BUTTON_SIZE.toFloat()
    val endSize = RIPPLE_MAX_SIZE.toFloat()
    val duration = ANIMATION_DURATION

    var currentSize by remember { mutableFloatStateOf(startSize) }
    var alpha by remember { mutableFloatStateOf(1f) }
    var color by remember { mutableStateOf(color) }

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
            color = color.copy(alpha = alpha),
            radius = currentSize,
            style = Stroke(width = 4.dp.toPx())
        )
    }
}