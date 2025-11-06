package com.imsproject.watch.view

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceAtMost
import com.imsproject.common.gameserver.GameType
import com.imsproject.watch.utils.UNDEFINED_ANGLE
import com.imsproject.watch.DARK_BACKGROUND_COLOR
import com.imsproject.watch.MY_ARC_SIZE
import com.imsproject.watch.MY_ARC_TOP_LEFT
import com.imsproject.watch.MY_STROKE_WIDTH
import com.imsproject.watch.MY_SWEEP_ANGLE
import com.imsproject.watch.OPPONENT_ARC_SIZE
import com.imsproject.watch.OPPONENT_ARC_TOP_LEFT
import com.imsproject.watch.OPPONENT_STROKE_WIDTH
import com.imsproject.watch.OPPONENT_SWEEP_ANGLE
import com.imsproject.watch.RUB_LOOP_TRACK
import com.imsproject.watch.SCREEN_CENTER
import com.imsproject.watch.SCREEN_RADIUS
import com.imsproject.watch.SILVER_COLOR
import com.imsproject.watch.utils.WavPlayerException
import com.imsproject.watch.view.WineGlassesActivity.Companion.TAG
import com.imsproject.watch.viewmodel.GameViewModel
import com.imsproject.watch.viewmodel.WineGlassesViewModel
import kotlinx.coroutines.delay

class WineGlassesActivity : GameActivity(GameType.WINE_GLASSES) {

    private val viewModel : WineGlassesViewModel by viewModels<WineGlassesViewModel>()


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
            GameViewModel.State.PLAYING -> WineGlasses(viewModel)
            else -> super.Main()
        }
    }

    companion object {
        const val TAG = "WineGlassesActivity"
    }
}

@Composable
fun WineGlasses(viewModel: WineGlassesViewModel) {
    val myArc = remember { viewModel.myArc }
    val opponentArc = remember { viewModel.opponentArc }
    val focusRequester = remember { FocusRequester() }
    var bezelWarningAlpha by remember { mutableFloatStateOf(0.0f) }
    var touchingBezel by remember { mutableStateOf(false) }
    val myReleased by viewModel.released.collectAsState()
    val opponentReleased by viewModel.opponentReleased.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = DARK_BACKGROUND_COLOR)
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
                            PointerEventType.Move, PointerEventType.Press -> {
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
    ) {

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
            val wavPlayer = viewModel.wavPlayer
            if(! myReleased) {
                var playing = false
                var inSync: Boolean
                while (true) {
                    try{
                        inSync = viewModel.inSync()
                        if (!playing && inSync) {
                            wavPlayer.playLooped(RUB_LOOP_TRACK)
                            playing = true
                        } else if (playing && !inSync) {
                            wavPlayer.pause(RUB_LOOP_TRACK)
                            playing = false
                        }
                        delay(16)
                    } catch(e: WavPlayerException){
                        Log.e(TAG, "WavPlayer Exception",e)
                        viewModel.onWavPlayerException()
                    }
                }
            } else if(wavPlayer.isPlaying(RUB_LOOP_TRACK)) {
                wavPlayer.stopFadeOut(RUB_LOOP_TRACK, 20)
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

        // =============== Draw background ================ |

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape = CircleShape)
                .background(color = SILVER_COLOR)
        )
        Box(
            modifier = Modifier
                .fillMaxSize(0.8f)
                .clip(shape = CircleShape)
                .background(color = DARK_BACKGROUND_COLOR)
                .shadow((SCREEN_RADIUS * 0.3f).dp, CircleShape, spotColor = Color.Red)
                .shadow(
                    (SCREEN_RADIUS * 0.3f).dp,
                    CircleShape,
                    spotColor = Color.Red.copy(alpha = 0.5f)
                )
        )

        // =================== Draw arcs =================== |

        Canvas(modifier = Modifier.fillMaxSize()) {

            // draw only if the touch point is within the defined borders
            if (myArc.startAngle.floatValue != UNDEFINED_ANGLE) {
                drawArc(
                    color = myArc.color.copy(alpha = myArc.currentAlpha),
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
                    color = opponentArc.color.copy(alpha = opponentArc.currentAlpha),
                    startAngle = opponentArc.startAngle.floatValue,
                    sweepAngle = OPPONENT_SWEEP_ANGLE,
                    useCenter = false,
                    topLeft = OPPONENT_ARC_TOP_LEFT,
                    size = OPPONENT_ARC_SIZE,
                    style = Stroke(width = OPPONENT_STROKE_WIDTH.dp.toPx())
                )
            }

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

