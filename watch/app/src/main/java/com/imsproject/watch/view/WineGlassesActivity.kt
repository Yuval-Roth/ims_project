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
import androidx.core.content.IntentSanitizer
import com.imsproject.watch.ARC_DEFAULT_ALPHA
import com.imsproject.watch.GLOWING_YELLOW_COLOR
import com.imsproject.watch.LIGHT_BLUE_COLOR
import com.imsproject.watch.LIGHT_GRAY_COLOR
import com.imsproject.watch.MARKER_FADE_DURATION
import com.imsproject.watch.MY_STROKE_WIDTH
import com.imsproject.watch.MAX_ANGLE_SKEW
import com.imsproject.watch.MIN_ANGLE_SKEW
import com.imsproject.watch.MY_ARC_SIZE
import com.imsproject.watch.MY_ARC_TOP_LEFT
import com.imsproject.watch.MY_RADIUS_INNER_EDGE
import com.imsproject.watch.MY_RADIUS_OUTER_EDGE
import com.imsproject.watch.MY_SWEEP_ANGLE
import com.imsproject.watch.OPPONENT_ARC_SIZE
import com.imsproject.watch.OPPONENT_ARC_TOP_LEFT
import com.imsproject.watch.OPPONENT_RADIUS_OUTER_EDGE
import com.imsproject.watch.OPPONENT_STROKE_WIDTH
import com.imsproject.watch.OPPONENT_SWEEP_ANGLE
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
        viewModel.onCreate(intent)
        setContent {
            Main()
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
        var angle = viewModel.angle.collectAsState().value // my angle
        val released = viewModel.released.collectAsState().value // my released state

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = DARK_BACKGROUND_COLOR)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val pointerEvent = awaitPointerEvent()
                            if (pointerEvent.type == PointerEventType.Release) {
                                viewModel.setReleased()
                            } else {
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

            //=============== Arc fade animation =============== |

            // arc fade animation - my arc

            LaunchedEffect(released) {
                if(viewModel.released.value){
                    val alphaAnimStep =  ARC_DEFAULT_ALPHA / (MARKER_FADE_DURATION / 16f)
                    while(viewModel.released.value && myArc.currentAlpha.floatValue > 0.0f){
                        myArc.currentAlpha.floatValue =
                            (myArc.currentAlpha.floatValue - alphaAnimStep)
                                .coerceAtLeast(0.0f)
                        delay(16)
                    }
                    myArc.previousAngle.floatValue = UNDEFINED_ANGLE
                    myArc.previousAngleDiff = 0f
                    myArc.startAngle.floatValue = UNDEFINED_ANGLE
                    myArc.direction = 0f
                    viewModel.setTouchPoint(-1.0,-1.0)
                } else {
                    myArc.currentAlpha.floatValue = ARC_DEFAULT_ALPHA
                }
            }

            // arc fade animation - opponent's arc
            LaunchedEffect(viewModel.opponentReleased.collectAsState().value) {
                if(viewModel.opponentReleased.value){
                    val alphaAnimStep =  ARC_DEFAULT_ALPHA / (MARKER_FADE_DURATION / 16f)
                    while(viewModel.opponentReleased.value && opponentArc.currentAlpha.floatValue > 0.0f){
                        opponentArc.currentAlpha.floatValue =
                            (opponentArc.currentAlpha.floatValue - alphaAnimStep)
                                .coerceAtLeast(0.0f)
                        delay(16)
                    }
                } else {
                    opponentArc.currentAlpha.floatValue = ARC_DEFAULT_ALPHA
                }
            }

            // =============== Draw background ================ |

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
            Box(
                modifier = Modifier
                    .size((OPPONENT_RADIUS_OUTER_EDGE * 0.65f).dp)
                    .clip(shape = CircleShape)
                    .background(color = GLOWING_YELLOW_COLOR)
            )

            // ======== Manipulate my arc based on touch point ========= |

            // only when touching the screen
            if(!released){

                // =========== for current iteration =============== |

                // calculate the skew angle to show the arc ahead of the finger
                // based on the calculations of the previous iteration
                val angleSkew = myArc.angleSkew
                myArc.startAngle.floatValue = angle + myArc.direction * angleSkew - MY_SWEEP_ANGLE / 2

                // ============== for next iteration =============== |

                // prepare the skew angle for the next iteration
                val previousAngle = myArc.previousAngle.floatValue
                val angleDiff = (angle - previousAngle).absoluteValue
                if(previousAngle != UNDEFINED_ANGLE){
                    val previousAngleDiff = myArc.previousAngleDiff
                    val angleDiffDiff = angleDiff - previousAngleDiff
                    myArc.angleSkew = if (angleDiffDiff > 1 && angleDiff > 2){
                        (angleSkew + 5f).coerceAtMost(MAX_ANGLE_SKEW)
                    } else if (angleDiffDiff < 1){
                        (angleSkew - 2.5f).coerceAtLeast(MIN_ANGLE_SKEW)
                    } else {
                        angleSkew
                    }
                }

                // prepare the direction for the next iteration
                if (previousAngle != UNDEFINED_ANGLE && angleDiff > 2){
                    val direction = myArc.direction
                    myArc.direction = if(angle > previousAngle){
                        (direction + 0.1f).coerceAtMost(1f)
                    } else if (angle < previousAngle){
                        (direction - 0.1f).coerceAtLeast(-1f)
                    } else {
                        direction
                    }
                    if(myArc.direction == 0f) myArc.angleSkew = MIN_ANGLE_SKEW
                }

                // current angle becomes previous angle for the next iteration
                myArc.previousAngle.floatValue = angle
                myArc.previousAngleDiff = angleDiff
            }

            // =================== Draw arcs =================== |

            Canvas(modifier = Modifier.fillMaxSize()) {

                // draw only if the touch point is within the defined borders
                if (viewModel.angle.value != UNDEFINED_ANGLE) {
                    drawArc(
                        color = GLOWING_YELLOW_COLOR.copy(alpha = myArc.currentAlpha.floatValue),
                        startAngle = myArc.startAngle.floatValue,
                        sweepAngle = MY_SWEEP_ANGLE,
                        useCenter = false,
                        topLeft = MY_ARC_TOP_LEFT,
                        size = MY_ARC_SIZE,
                        style = Stroke(width = MY_STROKE_WIDTH.dp.toPx())
                    )
                }

                // draw opponent's arc
                drawArc(
                    color = LIGHT_GRAY_COLOR.copy(alpha = opponentArc.currentAlpha.floatValue),
                    startAngle = opponentArc.startAngle.floatValue,
                    sweepAngle = OPPONENT_SWEEP_ANGLE,
                    useCenter = false,
                    topLeft = OPPONENT_ARC_TOP_LEFT,
                    size = OPPONENT_ARC_SIZE,
                    style = Stroke(width = OPPONENT_STROKE_WIDTH.dp.toPx())
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

