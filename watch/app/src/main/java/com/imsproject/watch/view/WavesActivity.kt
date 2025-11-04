package com.imsproject.watch.view

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush.Companion.horizontalGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import com.imsproject.common.gameserver.GameType
import com.imsproject.watch.BLUE_COLOR
import com.imsproject.watch.GRASS_GREEN_COLOR
import com.imsproject.watch.SCREEN_CENTER
import com.imsproject.watch.SCREEN_RADIUS
import com.imsproject.watch.WAVE_LEFT_STARTING_POSITION
import com.imsproject.watch.WAVE_OUTER_WIDTH
import com.imsproject.watch.WAVE_RIGHT_STARTING_POSITION
import com.imsproject.watch.utils.FlingTracker
import com.imsproject.watch.utils.OceanWaveEasing
import com.imsproject.watch.utils.cartesianToPolar
import com.imsproject.watch.viewmodel.GameViewModel
import com.imsproject.watch.viewmodel.WavesViewModel
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch


class WavesActivity: GameActivity(GameType.WAVES) {

    private val viewModel : WavesViewModel by viewModels<WavesViewModel>()

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
            GameViewModel.State.PLAYING -> Waves(viewModel)
            else -> super.Main()
        }
    }
}

@Composable
fun Waves(viewModel: WavesViewModel) {
    val density =  LocalDensity.current.density
    val tracker = remember { FlingTracker() }
    val wave by viewModel.wave.collectAsState()
    val turn by viewModel.turn.collectAsState()
    val turnMarkerAlpha = remember { Animatable(1f) }

    LaunchedEffect(wave.direction) {
        if(turn == 0){
            launch {
                turnMarkerAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = 100,
                        easing = LinearEasing,
                    )
                )
            }
        }
        val (start,end) = when(wave.direction){
            1 -> WAVE_LEFT_STARTING_POSITION.x to WAVE_RIGHT_STARTING_POSITION.x
            -1 -> WAVE_RIGHT_STARTING_POSITION.x to WAVE_LEFT_STARTING_POSITION.x
            0 -> {
                if(turn == viewModel.myDirection){
                    turnMarkerAlpha.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = 250,
                            easing = LinearEasing,
                        )
                    )
                }
                return@LaunchedEffect
            }
            else -> throw IllegalStateException("Wave direction must be either 1, 0 or -1")
        }
        val wavePosition = Animatable(start)
        wavePosition.animateTo(
            targetValue = end,
            animationSpec = tween(
                durationMillis = wave.animationLength,
                easing = OceanWaveEasing,
            )
        ){
            wave.topLeft = Offset(value, 0f)
        }
        viewModel.flipTurn()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        if (viewModel.myDirection == turn) {
                            val x = startOffset.x
                            val y = startOffset.y
                            val (distance, _) = cartesianToPolar(x, y)
                            if (distance > SCREEN_RADIUS * 0.7) {
                                tracker.startFling(x, y)
                            }
                        }
                    },
                    onDrag = { change: PointerInputChange, _ ->
                        val position = change.position
                        tracker.setOffset(position.x, position.y)
                    },
                    onDragEnd = {
                        if (tracker.started) {
                            val (nx, ny, speedPxPerSec) = tracker.endFling()
                            if (viewModel.myDirection * nx > 0) { // fling in my direction
                                val dpPecSec = speedPxPerSec / density
                                viewModel.fling(dpPecSec)
                            }
                        }
                    },
                    onDragCancel = { }
                )
            },
        contentAlignment = Alignment.Center
    ){
        // Outer part of the wave
        WaveWarpBox(
            amplitudePx = 5f,
            wavelengthPx = 180f,
            speedPxPerSec = 60f,
            phaseOffset = 0f,
        ) {
            Canvas(modifier = Modifier
                .fillMaxSize()
                .background(color = Color.White)
            ) {
                val brush = horizontalGradient(
                    colors = listOf(
                        Color.White,
                        Color(0xFF6BCBFF),
                        Color.White,
                    ),
                    startX = wave.topLeft.x,
                    endX = wave.topLeft.x + WAVE_OUTER_WIDTH,
                )
                drawRect(
                    brush = brush,
                    topLeft = wave.topLeft,
                    size = Size(WAVE_OUTER_WIDTH,SCREEN_RADIUS * 2),
                )
            }
        }
        Canvas(modifier = Modifier
            .fillMaxSize()
            .background(color = Color.Transparent)
        ) {

            // inner part of the wave
            val brush2 = horizontalGradient(
                colors = listOf(
                    Color.White,
                    Color(0xFFB1E4FF),
                    Color(0xFFB1E4FF),
                    Color(0xFFB1E4FF),
                    Color.White,
                ),
                startX = wave.topLeft.x + WAVE_OUTER_WIDTH *0.25f,
                endX = wave.topLeft.x + WAVE_OUTER_WIDTH *0.75f,
            )
            drawRect(
                brush = brush2,
                topLeft = Offset(wave.topLeft.x+ WAVE_OUTER_WIDTH *0.25f, 0f),
                size = Size(WAVE_OUTER_WIDTH *0.5f,SCREEN_RADIUS * 2),
                blendMode = BlendMode.ColorBurn
            )
            // ====================================== |

            // outer arc indicating the turn
            drawArc(
                color = Color.Gray,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = SCREEN_RADIUS * 0.2f),
            )
            val arcRadius = SCREEN_RADIUS * 0.95f
            val arcSize = Size(arcRadius * 2, arcRadius * 2)
            if(viewModel.myDirection == 1){
                drawArc(
                    color = BLUE_COLOR.copy(alpha = turnMarkerAlpha.value),
                    startAngle = -160f,
                    sweepAngle = -40f,
                    useCenter = false,
                    topLeft = Offset(
                        SCREEN_CENTER.x - arcRadius,
                        SCREEN_CENTER.y - arcRadius
                    ),
                    size = arcSize,
                    style = Stroke(width = SCREEN_RADIUS * 0.05f, cap = StrokeCap.Round)
                )
            } else if(viewModel.myDirection == -1){
                drawArc(
                    color = GRASS_GREEN_COLOR.copy(alpha = turnMarkerAlpha.value),
                    startAngle = -20f,
                    sweepAngle = 40f,
                    useCenter = false,
                    topLeft = Offset(
                        SCREEN_CENTER.x - arcRadius,
                        SCREEN_CENTER.y - arcRadius
                    ),
                    size = arcSize,
                    style = Stroke(width = SCREEN_RADIUS * 0.05f, cap = StrokeCap.Round)
                )
            }
        }
    }
}
// AGSL: horizontal sine displacement based on Y
private val AGSL = """
        uniform float2 resolution;
        uniform float amplitude;
        uniform float wavelength;
        uniform float omega;
        uniform float time;
        uniform float phase;
        
        // Declare the child shader with a NON-reserved name
        uniform shader content;
        
        half4 main(float2 fragCoord) {
            float2 uv = fragCoord;
            float y = uv.y;
        
            float k = 6.28318530718 / wavelength; // 2π/λ
            float xOffset = amplitude * sin(k * y - omega * time + phase);
        
            float2 sampleCoord = float2(
                clamp(uv.x + xOffset, 0.0, resolution.x - 1.0),
                clamp(uv.y, 0.0, resolution.y - 1.0)
            );
        
            // Sample via .eval(...)
            return content.eval(sampleCoord);
        }
    """.trimIndent()

@Composable
fun WaveWarpBox(
    modifier: Modifier = Modifier,
    amplitudePx: Float,
    wavelengthPx: Float,
    speedPxPerSec: Float,
    phaseOffset: Float = 0f,
    content: @Composable () -> Unit
) {
    // simple time driver
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            val nanos = awaitFrame() // suspend until next frame
            time = nanos / 1_000_000_000f
        }
    }

    Box(
        modifier = modifier.graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
            val shader = RuntimeShader(AGSL)
            shader.setFloatUniform("resolution", size.width, size.height)
            shader.setFloatUniform("amplitude", amplitudePx)
            shader.setFloatUniform("wavelength", wavelengthPx)
            val omega = (2f * Math.PI.toFloat()) * (speedPxPerSec / wavelengthPx)
            shader.setFloatUniform("omega", omega)
            shader.setFloatUniform("time", time)
            shader.setFloatUniform("phase", phaseOffset)
            renderEffect = RenderEffect
                .createRuntimeShaderEffect(shader, "content")
                .asComposeRenderEffect()
        }
    ) {
        content()
    }
}


