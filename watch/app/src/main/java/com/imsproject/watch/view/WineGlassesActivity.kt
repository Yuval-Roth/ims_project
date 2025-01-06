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
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentSanitizer
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import com.imsproject.watch.ALMOST_WHITE_COLOR
import com.imsproject.watch.ARC_DEFAULT_ALPHA
import com.imsproject.watch.CYAN_COLOR
import com.imsproject.watch.DARK_BACKGROUND_COLOR
import com.imsproject.watch.GLOWING_YELLOW_COLOR
import com.imsproject.watch.LIGHT_GRAY_COLOR
import com.imsproject.watch.MARKER_FADE_DURATION
import com.imsproject.watch.MIN_ANGLE_SKEW
import com.imsproject.watch.MY_ARC_SIZE
import com.imsproject.watch.MY_ARC_TOP_LEFT
import com.imsproject.watch.MY_STROKE_WIDTH
import com.imsproject.watch.MY_SWEEP_ANGLE
import com.imsproject.watch.OPPONENT_ARC_SIZE
import com.imsproject.watch.OPPONENT_ARC_TOP_LEFT
import com.imsproject.watch.OPPONENT_STROKE_WIDTH
import com.imsproject.watch.OPPONENT_SWEEP_ANGLE
import com.imsproject.watch.PACKAGE_PREFIX
import com.imsproject.watch.SCREEN_CENTER
import com.imsproject.watch.SCREEN_WIDTH
import com.imsproject.watch.UNDEFINED_ANGLE
import com.imsproject.watch.initProperties
import com.imsproject.watch.textStyle
import com.imsproject.watch.utils.WavPlayer
import com.imsproject.watch.view.contracts.Result
import com.imsproject.watch.viewmodel.GameViewModel
import com.imsproject.watch.viewmodel.WineGlassesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.imsproject.watch.R
import com.imsproject.watch.SCREEN_RADIUS

private const val LOW_BUILD_IN_TRACK = 0
private const val LOW_LOOP_TRACK = 1
private const val LOW_BUILD_OUT_TRACK = 2

class WineGlassesActivity : ComponentActivity() {

    private val viewModel : WineGlassesViewModel by viewModels<WineGlassesViewModel>()
    private lateinit var sound: WavPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val metrics = getSystemService(WindowManager::class.java).currentWindowMetrics
        initProperties(metrics.bounds.width(), metrics.bounds.height())
        viewModel.onCreate(intent,applicationContext)
        try{
            sound = WavPlayer(applicationContext)
            sound.load(LOW_BUILD_IN_TRACK, R.raw.wine_low_buildin)
            sound.load(LOW_LOOP_TRACK, R.raw.wine_low_loop)
            sound.load(LOW_BUILD_OUT_TRACK, R.raw.wine_low_buildout)
        } catch (e: IllegalArgumentException){
            val msg = e.message ?: "Unknown error"
            viewModel.exitWithError(msg, Result.Code.BAD_RESOURCE)
        }

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
                BlankScreen()
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
        val focusRequester = remember { FocusRequester() }
        var bezelWarningAlpha by remember { mutableFloatStateOf(0.0f) }
        var touchingBezel by remember { mutableStateOf(false) }
        var playSound by remember { mutableStateOf(false) }
        val released by viewModel.released.collectAsState() // my released state

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
                            touchingBezel = false
                            when (pointerEvent.type) {
                                PointerEventType.Move, PointerEventType.Press -> {
                                    val inputChange = pointerEvent.changes.first()
                                    inputChange.consume()
                                    val position = inputChange.position
                                    viewModel.setTouchPoint(
                                        position.x.toDouble(),
                                        position.y.toDouble()
                                    )
                                }

                                PointerEventType.Release -> {
                                    viewModel.setReleased()
                                }
                            }
                            playSound = true
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
                            bezelWarningAlpha = (bezelWarningAlpha + 0.01f).coerceAtMost(0.5f)
                            delay(16)
                        }
                        while(bezelWarningAlpha > 0.0f){
                            bezelWarningAlpha = (bezelWarningAlpha - 0.01f).coerceAtLeast(0.0f)
                            delay(16)
                        }
                    }
                } else {
                    while(bezelWarningAlpha > 0.0f){
                        bezelWarningAlpha = (bezelWarningAlpha - 0.01f).coerceAtLeast(0.0f)
                        delay(16)
                    }
                }
            }

            LaunchedEffect(released){
                if(playSound){
                    withContext(Dispatchers.IO){
                        if(released){
                            val currentlyPlaying = if(sound.isPlaying(LOW_LOOP_TRACK)){
                                LOW_LOOP_TRACK
                            } else {
                                LOW_BUILD_IN_TRACK
                            }
                            sound.stopFadeOut(currentlyPlaying,20)
                            sound.play(LOW_BUILD_OUT_TRACK)
                            playSound = false
                        } else {
                            sound.setVolume(LOW_BUILD_IN_TRACK,1.0f)
                            sound.play(LOW_BUILD_IN_TRACK) {
                                sound.setVolume(LOW_LOOP_TRACK,1.0f)
                                sound.playLooped(LOW_LOOP_TRACK)
                            }
                        }
                    }
                }
            }

            //=============== Arc fade animation =============== |

            // arc fade animation - my arc

            LaunchedEffect(released) {
                if(viewModel.released.value){
                    val alphaAnimStep =  ARC_DEFAULT_ALPHA / (MARKER_FADE_DURATION / 16f)
                    while(viewModel.released.value && myArc.currentAlpha > 0.0f){
                        myArc.currentAlpha =
                            (myArc.currentAlpha - alphaAnimStep)
                                .coerceAtLeast(0.0f)
                        delay(16)
                    }
                    myArc.previousAngle = UNDEFINED_ANGLE
                    myArc.previousAngleDiff = 0f
                    myArc.startAngle = UNDEFINED_ANGLE
                    myArc.direction = 0f
                    myArc.angleSkew = MIN_ANGLE_SKEW
                    viewModel.setTouchPoint(-1.0,-1.0)
                } else {
                    myArc.currentAlpha = ARC_DEFAULT_ALPHA
                }
            }

            // arc fade animation - opponent's arc
            LaunchedEffect(viewModel.opponentReleased.collectAsState().value) {
                if(viewModel.opponentReleased.value){
                    val alphaAnimStep =  ARC_DEFAULT_ALPHA / (MARKER_FADE_DURATION / 16f)
                    while(viewModel.opponentReleased.value && opponentArc.currentAlpha > 0.0f){
                        opponentArc.currentAlpha =
                            (opponentArc.currentAlpha - alphaAnimStep)
                                .coerceAtLeast(0.0f)
                        delay(16)
                    }
                } else {
                    opponentArc.currentAlpha = ARC_DEFAULT_ALPHA
                }
            }

            // =============== Draw background ================ |

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape = CircleShape)
                    .background(color = Color(0xFFC0C0C0))
            )
            Box(
                modifier = Modifier
                    .fillMaxSize(0.8f)
                    .clip(shape = CircleShape)
                    .background(color = DARK_BACKGROUND_COLOR)
                    .shadow((SCREEN_RADIUS* 0.3f).dp, CircleShape, spotColor = Color.Red)
                    .shadow((SCREEN_RADIUS* 0.3f).dp, CircleShape, spotColor = Color.Red.copy(alpha = 0.5f))
            )

            // =================== Draw arcs =================== |

            Canvas(modifier = Modifier.fillMaxSize()) {

                // draw only if the touch point is within the defined borders
                if (myArc.startAngle != UNDEFINED_ANGLE) {
                    drawArc(
                        color = GLOWING_YELLOW_COLOR.copy(alpha = myArc.currentAlpha),
                        startAngle = myArc.startAngle,
                        sweepAngle = MY_SWEEP_ANGLE,
                        useCenter = false,
                        topLeft = MY_ARC_TOP_LEFT,
                        size = MY_ARC_SIZE,
                        style = Stroke(width = MY_STROKE_WIDTH.dp.toPx())
                    )
                }

                // draw opponent's arc
                if (opponentArc.startAngle != UNDEFINED_ANGLE) {
                    drawArc(
                        color = CYAN_COLOR.copy(alpha = opponentArc.currentAlpha),
                        startAngle = opponentArc.startAngle,
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

    @Composable
    fun BlankScreen() {
        MaterialTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DARK_BACKGROUND_COLOR),
                contentAlignment = Alignment.Center
            ){

            }
        }
    }

    companion object {
        private const val TAG = "WineGlassesActivity"
    }
}

