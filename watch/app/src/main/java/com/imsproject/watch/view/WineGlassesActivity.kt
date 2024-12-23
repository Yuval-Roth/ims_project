package com.imsproject.watch.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import com.imsproject.watch.DARK_BACKGROUND_COLOR
import com.imsproject.watch.SCREEN_WIDTH
import com.imsproject.watch.initProperties
import com.imsproject.watch.textStyle
import androidx.compose.ui.input.pointer.PointerEventType
import com.imsproject.watch.GLOW_COLOR
import com.imsproject.watch.LIGHT_BLUE_COLOR
import com.imsproject.watch.MARKER_FADE_DURATION
import com.imsproject.watch.MARKER_SIZE
import com.imsproject.watch.MAX_ANGLE_SKEW
import com.imsproject.watch.MIN_ANGLE_SKEW
import com.imsproject.watch.UNDEFINED_ANGLE
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

class WineGlassesActivity : ComponentActivity() {

//    private val viewModel : WineGlassesViewModel by viewModels<WineGlassesViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val metrics = getSystemService(WindowManager::class.java).currentWindowMetrics
        initProperties(metrics.bounds.width(), metrics.bounds.height())
//        viewModel.onCreate(intent)
        setContent {
            WineGlasses()
//            Row(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .background(color = DARK_BACKGROUND_COLOR)
//                    .horizontalScroll(rememberScrollState()),
//                horizontalArrangement = Arrangement.Center
//            ) {
////                Main()
//
//            }
        }
    }

//    @Composable
//    fun Main(){
//        val state by viewModel.state.collectAsState()
//        when(state){
//            GameViewModel.State.LOADING -> {
//                LoadingScreen()
//            }
//            GameViewModel.State.PLAYING -> {
//                WineGlasses()
//            }
//            GameViewModel.State.TERMINATED -> {
//                val result = viewModel.resultCode.collectAsState().value
//                val intent = IntentSanitizer.Builder()
//                    .allowComponent(componentName)
//                    .build().sanitize(intent) {
//                        Log.d(TAG, "WineGlasses: $it")
//                    }
//                if(result != Result.Code.OK){
//                    intent.putExtra("$PACKAGE_PREFIX.error", viewModel.error.collectAsState().value)
//                }
//                setResult(result.ordinal,intent)
//                finish()
//            }
//        }
//    }

    @Composable
    private fun LoadingScreen() {
        MaterialTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DARK_BACKGROUND_COLOR),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        strokeWidth = (SCREEN_WIDTH.toFloat() * 0.02f).dp,
                        modifier = Modifier.size((SCREEN_WIDTH *0.2f).dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    BasicText(
                        text = "Loading session...",
                        style = textStyle
                    )
                }
            }
        }
    }

    @SuppressLint("ReturnFromAwaitPointerEventScope")
    @Composable
    fun WineGlasses() {
        var touchPoint = remember { mutableStateOf<Pair<Double,Double>>(Pair(-1.0,-1.0)) }

        // ============= touch point related values =========== |
        val center = remember {Offset(SCREEN_WIDTH / 2f, SCREEN_WIDTH / 2f)}
        val radiusOuterEdge = remember{ (SCREEN_WIDTH / 2).toFloat() }
        val radiusInnerEdge = remember { (SCREEN_WIDTH / 2) * 0.2f }
        val released = remember { mutableStateOf(false) }
        // ===================================================== |

        // ============= arc related values ================= |
        val defaultAlpha = remember { 0.8f }
        val alpha = remember { mutableFloatStateOf(0.8f) }
        val arcSize = remember { Size(radiusOuterEdge * 2, radiusOuterEdge * 2) }
        val sweepAngle = remember { 30f }
        var angleSkew = remember { 0f }
        var previousAngle = remember { UNDEFINED_ANGLE }
        var direction = remember { 0f }
        var arcStartAngle = remember { mutableFloatStateOf(UNDEFINED_ANGLE) }
        // ================================================== |

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = DARK_BACKGROUND_COLOR)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Release) {
                                released.value = true
                            } else {
                                released.value = false
                                val eventFirst = event.changes.first()
                                eventFirst.consume()
                                val touchPosition = eventFirst.position
                                touchPoint.value =
                                    Pair(touchPosition.x.toDouble(), touchPosition.y.toDouble())
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            LaunchedEffect(released.value) {
                if(released.value){
                    val alphaAnimStep =  defaultAlpha / (MARKER_FADE_DURATION / 16f)
                    while(released.value && alpha.floatValue > 0.0f){
                        alpha.floatValue = (alpha.floatValue - alphaAnimStep).coerceAtLeast(0.0f)
                        delay(16)
                    }
                    previousAngle = UNDEFINED_ANGLE
                    arcStartAngle.floatValue = UNDEFINED_ANGLE
                    touchPoint.value = Pair(-1.0,-1.0)
                    direction = 0f
                } else {
                    alpha.floatValue = defaultAlpha
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize(0.8f)
                    .clip(shape = CircleShape)
                    .background(color = LIGHT_BLUE_COLOR)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize(0.6f)
                    .clip(shape = CircleShape)
                    .background(color = DARK_BACKGROUND_COLOR)
            )
            Canvas(modifier = Modifier.fillMaxSize()) {

                val distanceFromCenter = sqrt(
                    (touchPoint.value.first - center.x).pow(2.0) +
                            (touchPoint.value.second - center.y).pow(2.0)
                )

                // animate only when touching the screen
                if(! released.value){
                    var angle = Math.toDegrees(
                        atan2(
                            touchPoint.value.second - center.y,
                            touchPoint.value.first - center.x
                        ).toDouble()
                    ).toFloat()
                    
                    // ==== angle logic ==== |
                    //make the angle move faster with faster change rate
                    val skewedAngle = angle + direction * angleSkew // for current iteration
                    if(previousAngle != UNDEFINED_ANGLE){ // for next iteration
                        angleSkew = if ((angle - previousAngle).absoluteValue > 3){
                            (angleSkew + 1f).coerceAtMost(MAX_ANGLE_SKEW)
                        } else {
                            (angleSkew - 0.5f).coerceAtLeast(MIN_ANGLE_SKEW)
                        }
                    }
                    // ====================== |

                    // ==== direction logic ==== |
                    if (previousAngle != UNDEFINED_ANGLE){
                        if(angle > previousAngle){
                            direction = (direction + 0.1f).coerceAtMost(1f)
                        } else if (angle < previousAngle){
                            direction = (direction - 0.1f).coerceAtLeast(-1f)
                        }
                    }
                    // =============================== |

                    previousAngle = angle
                    arcStartAngle.floatValue = skewedAngle - sweepAngle / 2
                }

                // draw only if the touch point is within the defined borders
                if (distanceFromCenter >= radiusInnerEdge && distanceFromCenter <= radiusOuterEdge) {
                    drawArc(
                        color = GLOW_COLOR.copy(alpha = alpha.floatValue),
                        startAngle = arcStartAngle.floatValue,
                        sweepAngle = sweepAngle,
                        useCenter = false, // Open arc, not filled
                        topLeft = Offset(
                            center.x - radiusOuterEdge,
                            center.y - radiusOuterEdge
                        ),
                        size = arcSize,
                        style = Stroke(width = MARKER_SIZE.dp.toPx()) // Adjust thickness
                    )
                }
            }
        }
    }

    companion object {
        private const val TAG = "WineGlassesActivity"
    }

    @Preview(device = "id:wearos_large_round", apiLevel = 34)
    @Composable
    fun PreviewLoadingScreen() {
        initProperties(454, 454)
        LoadingScreen()
    }

    @Preview(device = "id:wearos_large_round", apiLevel = 34)
    @Composable
    fun PreviewWineGlasses() {
        initProperties(454, 454)
        WineGlasses()
    }
}

