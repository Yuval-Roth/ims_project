package com.imsproject.watch.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.core.content.IntentSanitizer
import com.imsproject.watch.GLOWING_YELLOW_COLOR
import com.imsproject.watch.LIGHT_BLUE_COLOR
import com.imsproject.watch.MARKER_FADE_DURATION
import com.imsproject.watch.MARKER_SIZE
import com.imsproject.watch.MAX_ANGLE_SKEW
import com.imsproject.watch.MIN_ANGLE_SKEW
import com.imsproject.watch.MY_RADIUS_INNER_EDGE
import com.imsproject.watch.MY_RADIUS_OUTER_EDGE
import com.imsproject.watch.OPPONENT_RADIUS_OUTER_EDGE
import com.imsproject.watch.PACKAGE_PREFIX
import com.imsproject.watch.SCREEN_CENTER
import com.imsproject.watch.UNDEFINED_ANGLE
import com.imsproject.watch.viewmodel.GameViewModel
import com.imsproject.watch.viewmodel.WineGlassesViewModel
import com.imsproject.watch.view.contracts.Result
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

class WineGlassesActivity : ComponentActivity() {

    private val viewModel : WineGlassesViewModel by viewModels<WineGlassesViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val metrics = getSystemService(WindowManager::class.java).currentWindowMetrics
        initProperties(metrics.bounds.width(), metrics.bounds.height())
        viewModel.onCreate(intent)
        setContent {
            WineGlasses()
//            Main()
        }
    }

    @Composable
    fun Main(){
        val state by viewModel.state.collectAsState()
        when(state){
            GameViewModel.State.LOADING -> {
                LoadingScreen()
            }
            GameViewModel.State.PLAYING -> {
                WineGlasses()
            }
            GameViewModel.State.TERMINATED -> {
                val result = viewModel.resultCode.collectAsState().value
                val intent = IntentSanitizer.Builder()
                    .allowComponent(componentName)
                    .build().sanitize(intent) {
                        Log.d(TAG, "WineGlasses: $it")
                    }
                if(result != Result.Code.OK){
                    intent.putExtra("$PACKAGE_PREFIX.error", viewModel.error.collectAsState().value)
                }
                setResult(result.ordinal,intent)
                finish()
            }
        }
    }

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
        val myArc = remember { viewModel.myArc }
        val opponentArc = remember { viewModel.opponentArc }
        var touchPoint = viewModel.touchPoint.collectAsState().value

        // ============= touch point related values =========== |

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = DARK_BACKGROUND_COLOR)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val pointerEvent = awaitPointerEvent()
                            if (pointerEvent.type == PointerEventType.Release) {
                                viewModel.setReleased(true)
                            } else {
                                viewModel.setReleased(false)
                                val inputChange = pointerEvent.changes.first()
                                inputChange.consume()
                                val position = inputChange.position
                                viewModel.setTouchPoint(
                                    position.x.toDouble(),
                                    position.y.toDouble()
                                )
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {

            LaunchedEffect(viewModel.released.collectAsState().value) {
                if(viewModel.released.value){
                    val alphaAnimStep =  myArc.defaultAlpha / (MARKER_FADE_DURATION / 16f)
                    while(viewModel.released.value && myArc.currentAlpha.floatValue > 0.0f){
                        myArc.currentAlpha.floatValue =
                            (myArc.currentAlpha.floatValue - alphaAnimStep)
                                .coerceAtLeast(0.0f)
                        delay(16)
                    }
                    myArc.previousAngle.floatValue = UNDEFINED_ANGLE
                    myArc.startAngle.floatValue = UNDEFINED_ANGLE
                    myArc.direction = 0f
                    viewModel.setTouchPoint(-1.0,-1.0)
                } else {
                    myArc.currentAlpha.floatValue = myArc.defaultAlpha
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

                drawCircle(
                    color = GLOWING_YELLOW_COLOR,
                    radius = OPPONENT_RADIUS_OUTER_EDGE * 0.65f,
                    center = SCREEN_CENTER
                )

                val distanceFromCenter = sqrt(
                    (touchPoint.first - SCREEN_CENTER.x).pow(2.0) +
                            (touchPoint.second - SCREEN_CENTER.y).pow(2.0)
                )

                // animate only when touching the screen
                if(! viewModel.released.value){
                    val angle = Math.toDegrees(
                        atan2(
                            touchPoint.second - SCREEN_CENTER.y,
                            touchPoint.first - SCREEN_CENTER.x
                        ).toDouble()
                    ).toFloat()
                    
                    // ==== angle logic ==== |
                    //make the angle move faster with faster change rate
                    val angleSkew = myArc.angleSkew
                    val skewedAngle = angle + myArc.direction * angleSkew
                    // for next iteration
                    val previousAngle = myArc.previousAngle.floatValue
                    if(previousAngle != UNDEFINED_ANGLE){
                        val angleDiff = (angle - previousAngle).absoluteValue
                        println(angleDiff)
                        myArc.angleSkew = if (angleDiff > 3){
                            (angleSkew + 1f).coerceAtMost(MAX_ANGLE_SKEW)
                        } else if (angleDiff < 3 && angleDiff > 0){
                            (angleSkew - 0.5f).coerceAtLeast(MIN_ANGLE_SKEW)
                        } else {
                            angleSkew
                        }
                    }
                    // ====================== |

                    // ==== direction logic ==== |
                    if (previousAngle != UNDEFINED_ANGLE){
                        val direction = myArc.direction
                        myArc.direction = if(angle > previousAngle){
                            (direction + 0.1f).coerceAtMost(1f)
                        } else if (angle < previousAngle){
                            (direction - 0.1f).coerceAtLeast(-1f)
                        } else {
                            direction
                        }
                    }
                    // =============================== |

                    myArc.previousAngle.floatValue = angle
                    myArc.startAngle.floatValue = skewedAngle - myArc.sweepAngle / 2
                }

                // draw only if the touch point is within the defined borders
                if (distanceFromCenter >= MY_RADIUS_INNER_EDGE && distanceFromCenter <= MY_RADIUS_OUTER_EDGE) {
                    drawArc(
                        color = myArc.color.copy(alpha = myArc.currentAlpha.floatValue),
                        startAngle = myArc.startAngle.floatValue,
                        sweepAngle = myArc.sweepAngle,
                        useCenter = false, // Open arc, not filled
                        topLeft = Offset(
                            SCREEN_CENTER.x - MY_RADIUS_OUTER_EDGE,
                            SCREEN_CENTER.y - MY_RADIUS_OUTER_EDGE
                        ),
                        size = myArc.size,
                        style = Stroke(width = MARKER_SIZE.dp.toPx()) // Adjust thickness
                    )
                }

                // draw opponent's arc
                drawArc(
                    color = opponentArc.color,
                    startAngle = myArc.startAngle.floatValue - 15,
                    sweepAngle = 60f,
                    useCenter = false, // Open arc, not filled
                    topLeft = Offset(
                        SCREEN_CENTER.x - OPPONENT_RADIUS_OUTER_EDGE,
                        SCREEN_CENTER.y - OPPONENT_RADIUS_OUTER_EDGE
                    ),
                    size = opponentArc.size,
                    style = Stroke(width = MARKER_SIZE.dp.toPx()/4) // Adjust thickness
                )
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

