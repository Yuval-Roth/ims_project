package com.imsproject.watch.view

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.unit.dp
import com.imsproject.common.gameserver.GameType
import com.imsproject.watch.AXLE_HANDLE_LENGTH
import com.imsproject.watch.AXLE_WIDTH
import com.imsproject.watch.BEZIER_START_DISTANCE
import com.imsproject.watch.BROWN_COLOR
import com.imsproject.watch.CONTROL_POINT_DISTANCE
import com.imsproject.watch.DARK_BACKGROUND_COLOR
import com.imsproject.watch.GLOWING_YELLOW_COLOR
import com.imsproject.watch.LIGHT_BROWN_COLOR
import com.imsproject.watch.SCREEN_RADIUS
import com.imsproject.watch.STRETCH_PEAK_DECAY
import com.imsproject.watch.STRETCH_POINT_DISTANCE
import com.imsproject.watch.STRETCH_STEP
import com.imsproject.watch.initProperties
import com.imsproject.watch.utils.addToAngle
import com.imsproject.watch.utils.calculateTriangleThirdPoint
import com.imsproject.watch.utils.cartesianToPolar
import com.imsproject.watch.utils.isBetweenInclusive
import com.imsproject.watch.utils.isClockwise
import com.imsproject.watch.utils.polarToCartesian
import com.imsproject.watch.utils.sign
import com.imsproject.watch.viewmodel.FlourMillViewModel
import com.imsproject.watch.viewmodel.FlourMillViewModel.Axle
import com.imsproject.watch.viewmodel.FlourMillViewModel.AxleSide
import com.imsproject.watch.viewmodel.GameViewModel
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

class FlourMillActivity : GameActivity(GameType.FLOUR_MILL) {

    private val viewModel : FlourMillViewModel by viewModels<FlourMillViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val metrics = getSystemService(WindowManager::class.java).currentWindowMetrics
        initProperties(metrics.bounds.width(), metrics.bounds.height())
        viewModel.onCreate(intent,applicationContext)
        setupUncaughtExceptionHandler(viewModel)
        setContent {
            Main()
        }
    }

    @Composable
    fun Main(){
        val state by viewModel.state.collectAsState()
        when(state){
            GameViewModel.State.PLAYING -> {
                FlourMill()
            }
            else -> super.Main(viewModel)}
    }

    @Composable
    fun FlourMill() {
        val (centerX, centerY) = remember { polarToCartesian(0f, 0f) }
        val axle = remember { viewModel.axle }
        val mySide = remember { viewModel.myAxleSide }
        val focusRequester = remember { FocusRequester() }

        Box(
            modifier = Modifier.Companion
                .fillMaxSize()
                .background(color = DARK_BACKGROUND_COLOR)
                .onRotaryScrollEvent {
                    if (!viewModel.isCoolingDown()) {
                        val direction = if (it.verticalScrollPixels < 0) -1 else 1
                        viewModel.rotateMyAxleEnd(direction)
                    }
                    true
                }
                .focusRequester(focusRequester)
                .focusable()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            viewModel.onDragEnd()
                        },
                    ) { change, offset ->
                        if(viewModel.isDragged()) return@detectDragGestures
                        viewModel.dragged()

                        if (!viewModel.isCoolingDown()) {
                            val position = change.position
                            val (_, angle) = cartesianToPolar(
                                position.x.toDouble(),
                                position.y.toDouble()
                            )
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

            // =============== Animate the axle and its ends ================ |

            LaunchedEffect(Unit){
                while(true){
                    // left end
                    animateAxleEndStep(axle, AxleSide.LEFT, mySide)

                    // right end
                    animateAxleEndStep(axle, AxleSide.RIGHT, mySide)

                    // axle
                    if(axle.angle != axle.targetAngle){
                        val direction = getDirection(axle.angle, axle.targetAngle)
                        axle.angle = addToAngle(axle.angle, STRETCH_STEP * direction)
                    } else {
                        if(axle.isRotating){
                            axle.effectiveAngle = axle.angle
                            axle.isRotating = false
                            viewModel.resetCoolDown()
                        }
                    }
                    delay(16)
                }
            }

            // =============== Draw background ================ |
            Box(
                modifier = Modifier.Companion.fillMaxSize()
                    .background(color = BROWN_COLOR)
            )
            Box( // ground
                modifier = Modifier.Companion
                    .fillMaxSize(0.8f)
                    .clip(shape = CircleShape)
                    .background(color = LIGHT_BROWN_COLOR)
                    .shadow(elevation = (SCREEN_RADIUS*0.5).dp, CircleShape, spotColor = Color.Green)
                    .shadow(elevation = (SCREEN_RADIUS*0.5).dp, CircleShape, spotColor = Color.Green)
            )

            Box( // center axis
                modifier = Modifier.Companion
                    .fillMaxSize(0.3f)
                    .clip(shape = CircleShape)
                    .background(color = GLOWING_YELLOW_COLOR)
            )

            Box( // center axis filler
                modifier = Modifier.Companion
                    .fillMaxSize(0.200f)
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
                    val (stretchX,stretchY) = polarToCartesian(STRETCH_POINT_DISTANCE, axleEnd.angle)
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
                        quadraticTo(controlX, controlY, stretchX, stretchY)
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

    private fun animateAxleEndStep(
        axle: Axle,
        animatedSide: AxleSide,
        mySide : AxleSide
    ) {
        val axleEnd = axle.getEnd(animatedSide)

        // First thing we do is get the angle of the axle end to its target angle
        if (axleEnd.angle != axleEnd.targetAngle) {
            val direction = getDirection(axleEnd.angle, axleEnd.targetAngle)
            axleEnd.angle = addToAngle(axleEnd.angle, STRETCH_STEP * direction)
        }

        // If the axle end is at its target angle, we check if there is more work to be done
        else {

            // if the stretch peak is not zero, then we're either resetting or waiting for sync timeout
            if (axleEnd.stretchPeak != 0.0f) {

                if (axleEnd.resetting) {
                    // if the axle end is resetting, we set the target angle to be the next reset step
                    val baseAngle = axle.getEffectiveEndAngle(animatedSide)
                    val newStretchPeakSign = -1f * axleEnd.stretchPeak.sign()
                    val newStretchPeak = (axleEnd.stretchPeak.absoluteValue - STRETCH_PEAK_DECAY) * newStretchPeakSign
                    val newTargetAngle = addToAngle(baseAngle, newStretchPeak)
                    axleEnd.stretchPeak = newStretchPeak
                    axleEnd.targetAngle = newTargetAngle

                    // if the new stretch peak is zero, we finished resetting
                    if (newStretchPeak == 0.0f) {
                        axleEnd.resetting = false
                        if(animatedSide == mySide){
                            viewModel.resetCoolDown()
                        }
                    }
                }

                // we're waiting for the sync threshold timeout
                else {
                    // check if we passed the sync threshold timeout
                    if (System.currentTimeMillis() >= axleEnd.syncThresholdTimeout) {
                        if (viewModel.isSynced(animatedSide)) {
                            axleEnd.stretchPeak = 0.0f // leave the axle end at the target angle
                        } else {
                            axleEnd.resetting = true // reset the axle end to its base angle
                        }
                    }
                    //else ----> no work to be done
                }
            }
            else {
                if(!axle.isRotating && axleEnd.targetAngle != axle.getEffectiveEndAngle(animatedSide)){
                    // if we get here then the axle end went out of sync with the axle for some reason
                    // that i can't figure out. So we reset the axle end to its base angle according to the axle
                    axleEnd.targetAngle = axle.getEffectiveEndAngle(animatedSide)
                    if(animatedSide == mySide){
                        viewModel.resetCoolDown()
                    }
                }
                // else ----> no work to be done
            }
        }
    }

    private fun getDirection(angle: Float, targetAngle: Float) : Float {
        return if (isClockwise(angle, targetAngle)) 1f else -1f
    }

    companion object {
        private const val TAG = "FlourMillActivity"
    }
}