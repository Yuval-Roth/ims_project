package com.imsproject.watch.view

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import com.imsproject.common.gameserver.GameType
import com.imsproject.watch.SCREEN_RADIUS
import com.imsproject.watch.utils.FlingTracker
import com.imsproject.watch.utils.OceanWaveEasing
import com.imsproject.watch.utils.cartesianToPolar
import com.imsproject.watch.viewmodel.GameViewModel
import com.imsproject.watch.viewmodel.WavesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.math.pow

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
            GameViewModel.State.PLAYING -> Waves()
            else -> super.Main()
        }
    }

    @Composable
    fun Waves() {
        val density =  LocalDensity.current.density
        val scope = rememberCoroutineScope()
        val tracker = remember { FlingTracker() }
        val ovalPositions = remember { mutableStateListOf<MutableState<Offset>>() }
        val animationDirection by viewModel.animationDirection.collectAsState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            val x = startOffset.x
                            val y = startOffset.y
                            val (distance, _) = cartesianToPolar(x,y)
                            if (distance > SCREEN_RADIUS * 0.8){
                                tracker.startFling(x, y)
                            }
                        },
                        onDrag = { change: PointerInputChange, _ ->
                            val position = change.position
                            tracker.setOffset(position.x, position.y)
                        },
                        onDragEnd = {
                            if (tracker.started){
                                val (nx, ny, speedPxPerSec) = tracker.endFling()
                                if(viewModel.myDirection > 0 && nx > 0.5 || viewModel.myDirection < 0 && nx < -0.5){
                                    val dpPecSec = speedPxPerSec / density
                                    val animationLength = mapSpeedToDuration(dpPecSec, 1500, 5000)
                                    val ovalPosition = mutableStateOf(Offset(-SCREEN_RADIUS, SCREEN_RADIUS))
                                    val progress = Animatable(0f)
                                    ovalPositions.add(ovalPosition)
                                    scope.launch {
                                        progress.animateTo(
                                            targetValue = 1f,
                                            animationSpec = tween(
                                                durationMillis = animationLength,
                                                easing = OceanWaveEasing,
                                            )
                                        ){
                                            val mod = if (animationDirection > 0) 1f else -1f
                                            ovalPosition.value = if (animationDirection != 0) Offset( -mod*SCREEN_RADIUS * 2 + mod*2*SCREEN_RADIUS*value , -SCREEN_RADIUS * 0.5f)
                                            else Offset(-SCREEN_RADIUS, SCREEN_RADIUS)
                                        }
                                    }
                                }
                            }
                        },
                        onDragCancel = { }
                    )
                },
            contentAlignment = Alignment.Center
        ){
            Canvas(modifier = Modifier.fillMaxSize()) {
                ovalPositions.forEachIndexed { i, ovalPosition ->
                    drawOval(
                        color = if (i % 2 == 0) Color(0xFF3B82F6).copy(alpha = 0.8f)
                                else Color(0xFFFFD8D8).copy(alpha = 0.8f),
                        topLeft = ovalPosition.value,
                        size = Size(SCREEN_RADIUS * 2f, SCREEN_RADIUS * 3f),
                    )
                }
            }
        }
    }

    fun mapSpeedToDuration(pxPerSec: Float, minDurationMs: Int, maxDurationMs: Int): Int {
        val duration = if (pxPerSec <= 1000f) {
            maxDurationMs
        } else {
            val v = 1000f / pxPerSec
            val duration = maxDurationMs * v.pow(1.5f)
            duration.toInt().coerceIn(minDurationMs, maxDurationMs)
        }
        return duration
    }
}
