package com.imsproject.watch.view

import androidx.compose.ui.graphics.Path
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentSanitizer
import androidx.lifecycle.viewModelScope
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import com.imsproject.watch.AXLE_HANDLE_LENGTH
import com.imsproject.watch.AXLE_WIDTH
import com.imsproject.watch.BEZIER_START_DISTANCE
import com.imsproject.watch.CONTROL_POINT_DISTANCE
import com.imsproject.watch.DARK_BACKGROUND_COLOR
import com.imsproject.watch.FLOUR_MILL_SYNC_TIME_THRESHOLD
import com.imsproject.watch.GLOWING_YELLOW_COLOR
import com.imsproject.watch.LIGHT_BLUE_COLOR
import com.imsproject.watch.PACKAGE_PREFIX
import com.imsproject.watch.SCREEN_WIDTH
import com.imsproject.watch.STRETCH_PEAK
import com.imsproject.watch.STRETCH_PEAK_DECAY
import com.imsproject.watch.STRETCH_POINT_DISTANCE
import com.imsproject.watch.STRETCH_STEP
import com.imsproject.watch.initProperties
import com.imsproject.watch.textStyle
import com.imsproject.watch.utils.WaitMonitor
import com.imsproject.watch.utils.addToAngle
import com.imsproject.watch.utils.calculateAngleDiff
import com.imsproject.watch.utils.polarToCartesian
import com.imsproject.watch.utils.calculateTriangleThirdPoint
import com.imsproject.watch.utils.cartesianToPolar
import com.imsproject.watch.utils.isBetweenInclusive
import com.imsproject.watch.utils.isInQuadrant
import com.imsproject.watch.view.contracts.Result
import com.imsproject.watch.viewmodel.FlourMillViewModel
import com.imsproject.watch.viewmodel.FlourMillViewModel.Axle
import com.imsproject.watch.viewmodel.FlourMillViewModel.AxleEnd
import com.imsproject.watch.viewmodel.FlourMillViewModel.AxleSide
import com.imsproject.watch.viewmodel.GameViewModel
import kotlinx.coroutines.delay

class FlourMillActivity : ComponentActivity() {

    private val viewModel : FlourMillViewModel by viewModels<FlourMillViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val metrics = getSystemService(WindowManager::class.java).currentWindowMetrics
        initProperties(metrics.bounds.width(), metrics.bounds.height())
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
                FlourMill()
            }
            GameViewModel.State.TERMINATED -> {
                BlankScreen()
                val result = viewModel.resultCode.collectAsState().value
                val intent = IntentSanitizer.Builder()
                    .allowComponent(componentName)
                    .build().sanitize(intent) {
                        Log.d(TAG, "FlourMill: $it")
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
                modifier = Modifier.Companion
                    .fillMaxSize()
                    .background(DARK_BACKGROUND_COLOR),
                contentAlignment = Alignment.Companion.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.Companion.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        strokeWidth = (SCREEN_WIDTH.toFloat() * 0.02f).dp,
                        modifier = Modifier.Companion.size((SCREEN_WIDTH * 0.2f).dp)
                    )
                    Spacer(modifier = Modifier.Companion.height(16.dp))
                    BasicText(
                        text = "Loading session...",
                        style = textStyle
                    )
                }
            }
        }
    }

    @Composable
    fun FlourMill() {
        val (centerX, centerY) = remember { polarToCartesian(0f, 0f) }
        val axle = remember { viewModel.axle }
        val myAxleEnd = remember { viewModel.myAxleSide }
        val focusRequester = remember { FocusRequester() }
        val leftMonitor = remember { viewModel.leftMonitor }
        val rightMonitor = remember { viewModel.rightMonitor }
        val coolingDown = viewModel.coolingDown.collectAsState().value
        val leftCounter = viewModel.leftCounter.collectAsState().value
        val rightCounter = viewModel.rightCounter.collectAsState().value
        val axleCounter = viewModel.axleCounter.collectAsState().value

        Box(
            modifier = Modifier.Companion
                .fillMaxSize()
                .background(color = DARK_BACKGROUND_COLOR)
                .onRotaryScrollEvent {
                    if (!coolingDown) {
                        val direction = if (it.verticalScrollPixels < 0) -1 else 1
                        viewModel.rotateMyAxleEnd(direction)
                    }
                    true
                }
                .focusRequester(focusRequester)
                .focusable()
                .pointerInput(Unit) {
                    detectDragGestures { change, offset ->
                        val coolingDown = viewModel.coolingDown.value
                        if (!coolingDown) {
                            val position = change.position
                            val (_, angle) = cartesianToPolar(position.x.toDouble(), position.y.toDouble())
                            val direction = if (angle.isBetweenInclusive(-135.0001f, -45f)) {
                                if (offset.x < 0) -1 else 1
                            } else if (angle.isBetweenInclusive(-45.0001f, 45f)) {
                                if (offset.y > 0) 1 else -1
                            } else if (angle.isBetweenInclusive(45.0001f, 135f)) {
                                if (offset.x < 0) 1 else -1
                            } else {
                                if (offset.y > 0) -1 else 1
                            }
                            viewModel.rotateMyAxleEnd(direction)
                        }
                    }
                }
            ,
            contentAlignment = Alignment.Companion.Center
        ) {

            // Need to capture focus to enable rotary scroll event handling
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            // =============== Animate axle end stretch ================ |

            // Left end
            LaunchedEffect(leftCounter) {
                val inSync = animateAxleEnd(axle,AxleSide.LEFT,leftMonitor)
                if(! inSync && myAxleEnd == AxleSide.LEFT) {
                    delay(16)
                    viewModel.cooledDown()
                }
            }

            // Right end
            LaunchedEffect(rightCounter) {
                val inSync = animateAxleEnd(axle,AxleSide.RIGHT,rightMonitor)
                if(! inSync && myAxleEnd == AxleSide.RIGHT) {
                    delay(16)
                    viewModel.cooledDown()
                }
            }

            // axle rotation
            LaunchedEffect(axleCounter) {
                leftMonitor.wakeup()
                rightMonitor.wakeup()
                val targetAngle = addToAngle(axle.angle, STRETCH_PEAK * axle.toRotateDirection)
                val step = STRETCH_STEP * axle.toRotateDirection
                while(calculateAngleDiff(axle.angle, targetAngle) > 0.0f){
                    axle.angle = addToAngle(axle.angle, step)
                    delay(32)
                }
                axle.effectiveAngle = targetAngle
                delay(16)
                viewModel.cooledDown()
            }

            // =============== Draw background ================ |

            Box( // ground
                modifier = Modifier.Companion
                    .fillMaxSize(0.8f)
                    .clip(shape = CircleShape)
                    .background(color = LIGHT_BLUE_COLOR)
            )

            Box( // center axis
                modifier = Modifier.Companion
                    .fillMaxSize(0.2f)
                    .clip(shape = CircleShape)
                    .background(color = GLOWING_YELLOW_COLOR)
            )

            Box( // center axis filler
                modifier = Modifier.Companion
                    .fillMaxSize(0.125f)
                    .clip(shape = CircleShape)
                    .background(color = DARK_BACKGROUND_COLOR)
            )

            // =============== Draw axle ================ |

            Canvas(modifier = Modifier.Companion.fillMaxSize()) {
                for(side in AxleSide.entries){
                    val axleEnd = axle.getEnd(side)
                    val endAngle = axle.getEndAngle(side)

                    val (bezierStartX,bezierStartY) = polarToCartesian(BEZIER_START_DISTANCE, endAngle)
                    val (controlX,controlY) = polarToCartesian(CONTROL_POINT_DISTANCE, endAngle)
                    val (stretchX,stretchY) = polarToCartesian(STRETCH_POINT_DISTANCE, axleEnd.stretchPointAngle)
                    val (handleStartX,handleStartY) = calculateTriangleThirdPoint(
                        centerX, centerY,
                        stretchX, stretchY,
                        AXLE_HANDLE_LENGTH / 2
                    )
                    val (handleEndX,handleEndY) = calculateTriangleThirdPoint(
                        centerX, centerY,
                        stretchX, stretchY,
                        -AXLE_HANDLE_LENGTH / 2
                    )

                    val axlePath = Path().apply {
                        moveTo(centerX, centerY)
                        lineTo(bezierStartX, bezierStartY)
                        quadraticBezierTo(controlX, controlY, stretchX, stretchY)
                    }

                    val axleEndPath = Path().apply {
                        moveTo(handleStartX,handleStartY)
                        lineTo(handleEndX,handleEndY)
                    }

                    drawPath(
                        path = axlePath,
                        color = GLOWING_YELLOW_COLOR,
                        style = Stroke(width = AXLE_WIDTH)
                    )

                    drawPath(
                        path = axleEndPath,
                        color = axleEnd.handleColor,
                        style = Stroke(width = AXLE_WIDTH)
                    )
                }
            }
        }
    }

    private suspend fun animateAxleEnd(axle: Axle, side: AxleSide, monitor: WaitMonitor) : Boolean {
        val axleEnd = axle.getEnd(side)
        val direction = axleEnd.direction
        var referenceAngle = axle.getEffectiveEndAngle(side)

        // initial stretch in the direction of the scroll
        var targetAngle = addToAngle(referenceAngle, axleEnd.stretchPeak * direction)
        animateAxleEndStretch(axleEnd, targetAngle, direction)

        // wait to see if the scroll in sync with the opponent
        monitor.wait(FLOUR_MILL_SYNC_TIME_THRESHOLD)

        return if(viewModel.isSynced(side)){
            axleEnd.stretchPeak = 0.0f
            true
        } else {
            // the opponent did not scroll, so sync was not achieved.
            // so we add the spring effect.
            axleEnd.stretchPeak -= STRETCH_PEAK_DECAY
            while (axleEnd.stretchPeak > 0.0f) {
                // stretch in the opposite direction of the scroll

                targetAngle = addToAngle(referenceAngle, axleEnd.stretchPeak * -direction)
                animateAxleEndStretch(axleEnd, targetAngle, -direction)
                axleEnd.stretchPeak -= STRETCH_PEAK_DECAY

                // stretch in the direction of the scroll

                targetAngle = addToAngle(referenceAngle, axleEnd.stretchPeak * direction)
                animateAxleEndStretch(axleEnd, targetAngle, direction)
                axleEnd.stretchPeak -= STRETCH_PEAK_DECAY
            }
            false
        }
    }

    private suspend fun animateAxleEndStretch(
        axleEnd: AxleEnd,
        targetAngle: Float,
        direction : Int
    ) {
        while (calculateAngleDiff(axleEnd.stretchPointAngle, targetAngle) > 0.0f) {
            val newAngle = addToAngle(axleEnd.stretchPointAngle, STRETCH_STEP * direction)
            axleEnd.stretchPointAngle = newAngle
            delay(16)
        }
    }

    @Composable
    fun BlankScreen() {
        MaterialTheme {
            Box(
                modifier = Modifier.Companion
                    .fillMaxSize()
                    .background(DARK_BACKGROUND_COLOR),
                contentAlignment = Alignment.Companion.Center
            ) {

            }
        }
    }

    companion object {
        private const val TAG = "FlourMillActivity"
    }
}