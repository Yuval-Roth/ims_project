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

    var ripples = remember { viewModel.ripples }
    val sizeAnimStep = remember { (RIPPLE_MAX_SIZE - BUTTON_SIZE) / (ANIMATION_DURATION / 16f) }
    val alphaAnimStep = remember { 1f / (ANIMATION_DURATION / 16f) }
    val counter by viewModel.counter.collectAsState()

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
            // button content
        }

        // ================================================= |
        // =============== Ripple Effect =================== |
        // ================================================= |

        LaunchedEffect(counter) {
            for (ripple in ripples) {

                // ================================================= |
                // we add new ripples at the beginning of the list
                // in this code, we reached ripples that are already at max size
                // and we don't want to animate them anymore
                // so we break the loop
                if(ripple.size.floatValue >= RIPPLE_MAX_SIZE) break
                // ================================================= |

                // animate the ripples that are not at max size
                launch {
                    while (ripple.size.floatValue < RIPPLE_MAX_SIZE) {
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