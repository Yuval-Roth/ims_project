package com.imsproject.watch.view

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.imsproject.common.gameserver.GameType
import com.imsproject.watch.LIGHT_BROWN_COLOR
import com.imsproject.watch.SCREEN_RADIUS
import com.imsproject.watch.viewmodel.FlourMillViewModel
import com.imsproject.watch.viewmodel.GameViewModel
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceAtMost
import com.imsproject.common.utils.Angle
import com.imsproject.common.utils.UNDEFINED_ANGLE
import com.imsproject.watch.CYAN_COLOR
import com.imsproject.watch.DARK_ORANGE_COLOR
import com.imsproject.watch.DEEP_BLUE_COLOR
import com.imsproject.watch.GLOWING_YELLOW_COLOR
import com.imsproject.watch.LIGHT_ORANGE_COLOR
import com.imsproject.watch.LOW_LOOP_TRACK
import com.imsproject.watch.MILL_SOUND_TRACK
import com.imsproject.watch.MY_ARC_SIZE
import com.imsproject.watch.MY_ARC_TOP_LEFT
import com.imsproject.watch.MY_STROKE_WIDTH
import com.imsproject.watch.MY_SWEEP_ANGLE
import com.imsproject.watch.OPPONENT_ARC_SIZE
import com.imsproject.watch.OPPONENT_ARC_TOP_LEFT
import com.imsproject.watch.OPPONENT_STROKE_WIDTH
import com.imsproject.watch.OPPONENT_SWEEP_ANGLE
import com.imsproject.watch.R
import com.imsproject.watch.SCREEN_CENTER
import com.imsproject.watch.SILVER_COLOR
import com.imsproject.watch.utils.FlourPile
import com.imsproject.watch.utils.fastCoerceAtMost
import com.imsproject.watch.utils.polarToCartesian
import kotlinx.coroutines.delay

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
        // flour related
        var flourImageBitmap = remember<ImageBitmap?> { null }
        if(flourImageBitmap == null) { flourImageBitmap = ImageBitmap.imageResource(id = R.drawable.flour) }
        val scaledFlourWidth = remember { (flourImageBitmap.width * 0.15f).toInt() }
        val scaledFlourHeight = remember { (flourImageBitmap.height * 0.15f).toInt() }
        val flourPile = remember { FlourPile() }
        val fallingFlour = remember { mutableStateListOf<MutableFloatState>() }

        // arc related
        val myArc = remember { viewModel.myArc }
        val opponentArc = remember { viewModel.opponentArc }
        val myReleased by viewModel.released.collectAsState()
        val opponentReleased by viewModel.opponentReleased.collectAsState()

        // wheel related
        var currentWheelAngle by remember { mutableStateOf(viewModel.targetWheelAngle.value) }
        var angleChangeSum = remember { 0f }

        // bezel warning related
        val focusRequester = remember { FocusRequester() }
        var bezelWarningAlpha by remember { mutableFloatStateOf(0.0f) }
        var touchingBezel by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onRotaryScrollEvent {
                    touchingBezel = true
                    true
                }
                .focusRequester(focusRequester)
                .focusable()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val pointerEvent = awaitPointerEvent()
                            val inputChange = pointerEvent.changes.first()
                            inputChange.consume()
                            touchingBezel = false
                            when (pointerEvent.type) {
                                PointerEventType.Press, PointerEventType.Move -> {
                                    val position = inputChange.position
                                    viewModel.setTouchPoint(position.x, position.y)
                                }

                                PointerEventType.Release -> {
                                    viewModel.setTouchPoint(-1.0f, -1.0f)
                                }
                            }
                        }
                    }
                }
            ,
            contentAlignment = Alignment.Center

        ){

            // ================== Scrolling bezel warning ================== |

            LaunchedEffect(Unit){
                focusRequester.requestFocus()
            }

            LaunchedEffect(touchingBezel) {
                if(touchingBezel){
                    bezelWarningAlpha = 0.0f
                    while(touchingBezel){
                        while(bezelWarningAlpha < 0.5f){
                            bezelWarningAlpha = (bezelWarningAlpha + 0.01f).fastCoerceAtMost(0.5f)
                            delay(16)
                        }
                        while(bezelWarningAlpha > 0.0f){
                            bezelWarningAlpha = (bezelWarningAlpha - 0.01f).fastCoerceAtLeast(0.0f)
                            delay(16)
                        }
                    }
                } else {
                    while(bezelWarningAlpha > 0.0f){
                        bezelWarningAlpha = (bezelWarningAlpha - 0.01f).fastCoerceAtLeast(0.0f)
                        delay(16)
                    }
                }
            }

            // ================== Sound effects ================== |

            // play high sound when in sync
            LaunchedEffect(myReleased){
                val sound = viewModel.wavPlayer
                if(! myReleased) {
                    var playing = false
                    var inSync: Boolean
                    while (true) {
                        inSync = viewModel.inSync
                        if (!playing && inSync) {
                            sound.playLooped(MILL_SOUND_TRACK)
                            playing = true
                        } else if (playing && !inSync) {
                            sound.pause(MILL_SOUND_TRACK)
                            playing = false
                        }
                        delay(16)
                    }
                } else if(sound.isPlaying(MILL_SOUND_TRACK)) {
                    sound.stopFadeOut(MILL_SOUND_TRACK, 20)
                }
            }

            //=============== Arc fade animation =============== |

            // arc fade animation - my arc

            LaunchedEffect(myReleased) {
                if(myReleased){
                    myArc.fadeOut()
                    myArc.reset()
                } else {
                    myArc.show()
                }
            }

            // arc fade animation - opponent's arc
            LaunchedEffect(opponentReleased) {
                if(opponentReleased){
                    opponentArc.fadeOut()
                } else {
                    opponentArc.show()
                }
            }

            // =============== Wheel rotation ================= |

            LaunchedEffect(Unit) {
                while(true){
                    val targetAngle = viewModel.targetWheelAngle.value
                    if(targetAngle != currentWheelAngle){
                        val direction = if(Angle.isClockwise(currentWheelAngle,targetAngle)) 1 else -1
                        val diff = targetAngle - currentWheelAngle
                        if(diff < 1f) {
                            currentWheelAngle = targetAngle
                            angleChangeSum += diff
                        } else if (1f <= diff && diff < 4f) {
                            currentWheelAngle += 1f * direction
                            angleChangeSum += 1f
                        } else if(4f <= diff && diff < 8f) {
                            currentWheelAngle += 2f * direction
                            angleChangeSum += 2f
                        } else if(8f <= diff && diff < 16f) {
                            currentWheelAngle += 4f * direction
                            angleChangeSum += 4f
                        } else {
                            currentWheelAngle += 8f * direction
                            angleChangeSum += 8f
                        }
                        if(angleChangeSum > 360f) {
                            angleChangeSum -= 360f
                            fallingFlour.add(mutableFloatStateOf(0.46f))
                        }
                    }
                    delay(16)
                }
            }

            // =============== Falling flour ================= |

            LaunchedEffect(Unit) {
                while(true){
                    for(relativeRadius in fallingFlour){
                        relativeRadius.floatValue += 0.015f
                        if(relativeRadius.floatValue > 0.71f){
                            fallingFlour.remove(relativeRadius)
                            if(!flourPile.isFull()){
                                flourPile.addNext()
                            }
                        }
                    }
                    delay(16)
                }
            }

            // ================== Draw the screen ================== |

            Box(
                modifier = Modifier.Companion
                    .fillMaxSize()
                    .background(color = SILVER_COLOR)
            )
            Box( // ground
                modifier = Modifier.Companion
                    .fillMaxSize(0.8f)
                    .clip(shape = CircleShape)
                    .background(color = LIGHT_BROWN_COLOR)
            )
            Image(
                painter = painterResource(id = R.drawable.mill),
                contentDescription = null,
                modifier = Modifier
                    .size((SCREEN_RADIUS * 0.5f).dp),
                contentScale = ContentScale.FillBounds
            )

            val (x,y) = remember { polarToCartesian(SCREEN_RADIUS * 0.75f, 90.0) }
            Canvas(modifier = Modifier.fillMaxSize()){

                // draw the falling flour
                for(relativeRadius in fallingFlour){
                    val (x,y) = polarToCartesian(SCREEN_RADIUS * relativeRadius.floatValue, 90.0)
                    drawImage(
                        image = flourImageBitmap,
                        dstSize = IntSize(scaledFlourWidth, scaledFlourHeight),
                        dstOffset = IntOffset(x.toInt(), y.toInt()),
                    )
                }

                // draw the flour pile
                var rowSize = 13
                for(i in 0 until 6){
                    for (j in 0 until rowSize) {
                        if (flourPile.get(i, j)) {
                            drawImage(
                                image = flourImageBitmap,
                                dstSize = IntSize(scaledFlourWidth, scaledFlourHeight),
                                dstOffset = IntOffset(x.toInt() - scaledFlourWidth / 2 + (j - (rowSize / 2)) * 7, y.toInt() - scaledFlourHeight / 2 - i * 3),
                            )
                        }
                    }
                    rowSize -= 2
                }

                // draw the wheel
                rotate(currentWheelAngle.floatValue, pivot = SCREEN_CENTER) {
                    drawWheel()
                }

                // draw only if the touch point is within the defined borders
                if (myArc.startAngle.floatValue != UNDEFINED_ANGLE) {
                    drawArc(
                        color = GLOWING_YELLOW_COLOR.copy(alpha = myArc.currentAlpha),
                        startAngle = myArc.startAngle.floatValue,
                        sweepAngle = MY_SWEEP_ANGLE,
                        useCenter = false,
                        topLeft = MY_ARC_TOP_LEFT,
                        size = MY_ARC_SIZE,
                        style = Stroke(width = MY_STROKE_WIDTH.dp.toPx())
                    )
                }

                // draw opponent's arc
                if (opponentArc.startAngle.floatValue != UNDEFINED_ANGLE) {
                    drawArc(
                        color = CYAN_COLOR.copy(alpha = opponentArc.currentAlpha),
                        startAngle = opponentArc.startAngle.floatValue,
                        sweepAngle = OPPONENT_SWEEP_ANGLE,
                        useCenter = false,
                        topLeft = OPPONENT_ARC_TOP_LEFT,
                        size = OPPONENT_ARC_SIZE,
                        style = Stroke(width = OPPONENT_STROKE_WIDTH.dp.toPx())
                    )
                }

                // draw the bezel warning
                if(touchingBezel){
                    drawCircle(
                        color = Color.Red.copy(alpha = bezelWarningAlpha),
                        radius = SCREEN_RADIUS,
                        center = SCREEN_CENTER,
                        style = Stroke(width = (SCREEN_RADIUS * 0.1f).dp.toPx())
                    )
                    drawCircle(
                        color = Color.Green.copy(alpha = bezelWarningAlpha),
                        radius = SCREEN_RADIUS - (SCREEN_RADIUS * 0.3f),
                        center = SCREEN_CENTER,
                        style = Stroke(width = (SCREEN_RADIUS * 0.1f).dp.toPx())
                    )
                }
            }
        }
    }

    fun DrawScope.drawWheel() {
        // draw the spokes
        var angle = Angle(45f)
        repeat(4) {
            val (x1, y1) = polarToCartesian(SCREEN_RADIUS * 0.10f, angle)
            val (x2, y2) = polarToCartesian(SCREEN_RADIUS * 0.56f, angle)
            drawLine(
                color = DEEP_BLUE_COLOR,
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = (SCREEN_RADIUS * 0.04f).dp.toPx()
            )
            drawLine(
                color = DARK_ORANGE_COLOR,
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = (SCREEN_RADIUS * 0.02f).dp.toPx()
            )
            angle += 90f
        }

        // outer circle
        drawCircle(
            color = DEEP_BLUE_COLOR,
            radius = SCREEN_RADIUS * 0.6f,
            center = SCREEN_CENTER,
            style = Stroke(width = (SCREEN_RADIUS * 0.045f).dp.toPx())
        )
        drawCircle(
            color = LIGHT_ORANGE_COLOR,
            radius = SCREEN_RADIUS * 0.6f,
            center = SCREEN_CENTER,
            style = Stroke(width = (SCREEN_RADIUS * 0.025f).dp.toPx())
        )

        // inner circle
        drawCircle(
            color = DEEP_BLUE_COLOR,
            radius = SCREEN_RADIUS * 0.08f,
            center = SCREEN_CENTER,
            style = Stroke(width = (SCREEN_RADIUS * 0.05f).dp.toPx())
        )
        drawCircle(
            color = LIGHT_ORANGE_COLOR,
            radius = SCREEN_RADIUS * 0.08f,
            center = SCREEN_CENTER,
            style = Stroke(width = (SCREEN_RADIUS * 0.03f).dp.toPx())
        )

    }

    companion object {
        private const val TAG = "FlourMillActivity"
    }
}