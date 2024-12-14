package com.imsproject.watch.view

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentSanitizer
import androidx.wear.compose.material.Button
import com.imsproject.watch.WATER_RIPPLES_ANIMATION_DURATION
import com.imsproject.watch.WATER_RIPPLES_BUTTON_SIZE
import com.imsproject.watch.DARK_BACKGROUND_COLOR
import com.imsproject.watch.PACKAGE_PREFIX
import com.imsproject.watch.RIPPLE_MAX_SIZE
import com.imsproject.watch.model.MainModel
import com.imsproject.watch.view.contracts.Result
import com.imsproject.watch.viewmodel.WaterRipplesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WaterRipplesActivity : ComponentActivity() {

    private val viewModel : WaterRipplesViewModel by viewModels<WaterRipplesViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        viewModel.onCreate()
        setContent {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = DARK_BACKGROUND_COLOR)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.Center
            ) {
                // if the game is not playing, finish the activity
                if(! viewModel.playing.collectAsState().value){
                    val result = viewModel.resultCode.collectAsState().value
                    val intent = IntentSanitizer.Builder()
                        .allowComponent(componentName)
                        .build().sanitize(intent) {
                            Log.d(TAG, "WaterRipples: $it")
                        }
                    if(result != Result.Code.OK){
                        intent.putExtra("$PACKAGE_PREFIX.error", viewModel.error.collectAsState().value)
                    }
                    setResult(result.ordinal,intent)
                    finish()
                } else {
                    WaterRipples(viewModel)
                }
            }
        }
    }

    @Composable
    fun WaterRipples(viewModel: WaterRipplesViewModel) {

        var ripples = remember { viewModel.ripples }
        val counter by viewModel.counter.collectAsState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = DARK_BACKGROUND_COLOR),
            contentAlignment = Alignment.Center
        ) {

            Button(
                modifier = Modifier.size(WATER_RIPPLES_BUTTON_SIZE.dp),
                onClick = {
                    viewModel.click()
                },
            ){
                // button content
            }

            // ================================================= |
            // =============== Ripple Effect =================== |
            // ================================================= |

            LaunchedEffect(counter) {
                val rippleIterator = ripples.listIterator()
                while (rippleIterator.hasNext()) {
                    val ripple = rippleIterator.next()

                    // ================================================= |
                    // we add new ripples at the beginning of the list
                    // in this code, we reached ripples that are already at max size
                    // and we don't want to animate them anymore
                    // so we remove them from the list
                    if(ripple.size.floatValue >= RIPPLE_MAX_SIZE) {
                        rippleIterator.remove()
                        continue
                    }
                    // ================================================= |

                    // animate the ripple that is not at max size

                    val sizeAnimStep = (RIPPLE_MAX_SIZE - WATER_RIPPLES_BUTTON_SIZE) / (WATER_RIPPLES_ANIMATION_DURATION / 16f)
                    val alphaAnimStep =  ripple.startingAlpha / (WATER_RIPPLES_ANIMATION_DURATION / 16f)

                    launch {
                        while (ripple.size.floatValue < RIPPLE_MAX_SIZE) {
                            ripple.size.floatValue += sizeAnimStep
                            ripple.currentAlpha.floatValue = (ripple.currentAlpha.floatValue-alphaAnimStep).coerceAtLeast(0f)
                            delay(16)
                        }
                    }
                }
            }

            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                for(ripple in ripples){
                    val color = ripple.color
                    val size = ripple.size.floatValue
                    val alpha = ripple.currentAlpha.floatValue

                    drawCircle(
                        color = color.value.copy(alpha = alpha),
                        radius = size,
                        style = Stroke(width = 4.dp.toPx())
                    )
                }
            }
        }
    }

    companion object {
        private const val TAG = "WaterRipplesActivity"
    }
}

