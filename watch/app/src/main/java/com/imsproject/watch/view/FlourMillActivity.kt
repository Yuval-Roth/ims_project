package com.imsproject.watch.view

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.imsproject.common.gameserver.GameType
import com.imsproject.watch.AXLE_WIDTH
import com.imsproject.watch.BROWN_COLOR
import com.imsproject.watch.DARK_BACKGROUND_COLOR
import com.imsproject.watch.GLOWING_YELLOW_COLOR
import com.imsproject.watch.LIGHT_BROWN_COLOR
import com.imsproject.watch.SCREEN_RADIUS
import com.imsproject.watch.utils.cartesianToPolar
import com.imsproject.watch.utils.polarToCartesian
import com.imsproject.watch.viewmodel.FlourMillViewModel
import com.imsproject.watch.viewmodel.GameViewModel
import androidx.compose.ui.input.pointer.PointerEventType
import com.imsproject.watch.BRIGHT_CYAN_COLOR
import com.imsproject.watch.TOUCH_CIRCLE_RADIUS
import com.imsproject.watch.utils.Angle

class FlourMillActivity : GameActivity(GameType.FLOUR_MILL) {

    private val viewModel : FlourMillViewModel by viewModels<FlourMillViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.onCreate(viewModel)
        setContent {
            Main()
        }
    }

    @Composable
    override fun Main(){
        val state by viewModel.state.collectAsState()
        when(state){
            GameViewModel.State.PLAYING -> FlourMill()
            else -> super.Main()
        }
    }

    @Composable
    fun FlourMill() {
        val (centerX, centerY) = remember { polarToCartesian(0f, 0.0) }
        val axle = remember { viewModel.axle }
        var showAxle by remember { mutableStateOf(false) }
        val myTouchPoint = viewModel.myTouchPoint.collectAsState().value
        val opponentTouchPoint = viewModel.opponentTouchPoint.collectAsState().value

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val pointerEvent = awaitPointerEvent()
                            val inputChange = pointerEvent.changes.first()
                            inputChange.consume()
                            val position = inputChange.position
                            val (radius, angle) = cartesianToPolar(position.x, position.y)
                            val relativeRadius = radius / SCREEN_RADIUS
                            when (pointerEvent.type) {
                                PointerEventType.Press -> {
                                    axle.angle = angle + -90f
                                    viewModel.setTouchPoint(relativeRadius, angle)
                                    showAxle = true
                                }
                                PointerEventType.Move -> {
                                    viewModel.setTouchPoint(relativeRadius, angle)
                                }
                                PointerEventType.Release -> {
                                    viewModel.setTouchPoint(-1f, Angle.undefined())
                                    showAxle = false
                                }
                            }
                        }
                    }
                }
            ,
            contentAlignment = Alignment.Center

        ){
            Box(
                modifier = Modifier.Companion
                    .fillMaxSize()
                    .background(color = BROWN_COLOR)
            )
            Box( // ground
                modifier = Modifier.Companion
                    .fillMaxSize(0.8f)
                    .clip(shape = CircleShape)
                    .background(color = LIGHT_BROWN_COLOR)
                    .shadow(
                        elevation = (SCREEN_RADIUS * 0.5).dp,
                        CircleShape,
                        spotColor = Color.Green
                    )
                    .shadow(
                        elevation = (SCREEN_RADIUS * 0.5).dp,
                        CircleShape,
                        spotColor = Color.Green
                    )
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
            Canvas(modifier = Modifier.Companion.fillMaxSize()){
                for(touchPoint in listOf(myTouchPoint, opponentTouchPoint)){
                    if(touchPoint.first < 0f) continue

                    val (leftX, leftY) = polarToCartesian(SCREEN_RADIUS * 0.7f, axle.angle + 90f)
                    val (leftX2, leftY2) = polarToCartesian(SCREEN_RADIUS * 0.9f, axle.angle + 90f)
                    val (rightX, rightY) = polarToCartesian(SCREEN_RADIUS * 0.9f, axle.angle + -90f)

                    val touchPointDistance = touchPoint.first * SCREEN_RADIUS
                    val touchPointAngle = touchPoint.second
                    val touchPointInBounds = (touchPointAngle - (axle.angle + 90f) <= (360f / TOUCH_CIRCLE_RADIUS))
                            && touchPointDistance > (SCREEN_RADIUS * 0.7f - TOUCH_CIRCLE_RADIUS)
                            && touchPointDistance < (SCREEN_RADIUS * 0.9f + TOUCH_CIRCLE_RADIUS)

                    val (x,y) = polarToCartesian(touchPointDistance, touchPointAngle)

                    val axlePath = Path().apply {
                        moveTo(rightX,rightY)
                        lineTo(leftX, leftY)
                    }

                    val axlePath2 = Path().apply {
                        moveTo(leftX,leftY)
                        lineTo(leftX2, leftY2)
                    }

                    if(showAxle){
                        drawPath(
                            path = axlePath,
                            color = GLOWING_YELLOW_COLOR,
                            style = Stroke(width = AXLE_WIDTH)
                        )
                        drawPath(
                            path = axlePath2,
                            color = BRIGHT_CYAN_COLOR,
                            style = Stroke(width = AXLE_WIDTH)
                        )
                        drawCircle(
                            radius = TOUCH_CIRCLE_RADIUS,
                            center = Offset(x, y),
                            color = if (touchPointInBounds) BRIGHT_CYAN_COLOR else BRIGHT_CYAN_COLOR.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "FlourMillActivity"
    }
}