package com.imsproject.watch.view

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInExpo
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.EaseInSine
import androidx.compose.animation.core.EaseOutExpo
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush.Companion.horizontalGradient
import androidx.compose.ui.graphics.Color
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
import com.imsproject.watch.viewmodel.WavesViewModel.AnimationStage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.max

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
        val tracker = remember { FlingTracker() }
        val scope = rememberCoroutineScope()
        val waves = remember { viewModel.waves }
        val turn by viewModel.turn.collectAsState()

        LaunchedEffect(Unit) {
            snapshotFlow { viewModel.waves.lastOrNull() }
                .collect { wave ->
                    if (wave == null) return@collect
                    if (wave.animationStage == AnimationStage.NOT_STARTED){
                        scope.launch(Dispatchers.Main) {
                            wave.animationStage = AnimationStage.FIRST
                            val mod = if (wave.direction > 0) 1f else -1f
                            val progress = Animatable(0f)
                            progress.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(
                                    durationMillis = wave.animationLength,
                                    easing = OceanWaveEasing,
                                )
                            ){
                                wave.topLeft = Offset( -mod*(SCREEN_RADIUS * 2) + mod*2*SCREEN_RADIUS*value , -SCREEN_RADIUS * 0.5f)
                                wave.animationProgress = value
                            }
                            wave.animationStage = AnimationStage.SECOND
                            progress.animateTo(
                                targetValue = 0.1f,
                                animationSpec = tween(
                                    durationMillis = 200,
                                    easing = LinearEasing,
                                )
                            ){
                                wave.topLeft = Offset( mod*SCREEN_RADIUS * 2 + -mod*2*SCREEN_RADIUS*value , -SCREEN_RADIUS * 0.5f)
                                wave.animationProgress = value
                            }
                            viewModel.flipTurn()
                            wave.animationStage = AnimationStage.DONE
                        }
                    }
                }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            if (viewModel.myDirection == viewModel.turn.value) {
                                val x = startOffset.x
                                val y = startOffset.y
                                val (distance, _) = cartesianToPolar(x, y)
                                if (distance > SCREEN_RADIUS * 0.8) {
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
                                val myDirection = viewModel.myDirection
                                if (myDirection * nx > 0) { // fling in my direction
                                    val dpPecSec = speedPxPerSec / density
                                    viewModel.fling(dpPecSec, myDirection)
                                }
                            }
                        },
                        onDragCancel = { }
                    )
                },
            contentAlignment = Alignment.Center
        ){
            Canvas(modifier = Modifier.fillMaxSize()) {
                if(turn > 0){
                    val brush = horizontalGradient(
                        colors = listOf(
                            Color(0xFF3B82F6).copy(alpha = 0.0f),
                            Color(0xFF3B82F6)
                        ),
                        startX = -50f,
                        endX = 45f,
                    )
                    drawOval(
                        brush = brush,
                        topLeft = Offset(-SCREEN_RADIUS*2f + 45, -SCREEN_RADIUS * 0.5f),
                        size = Size(SCREEN_RADIUS * 2f, SCREEN_RADIUS * 3f)
                    )
                }
                if(turn < 0){
                    val brush = horizontalGradient(
                        colors = listOf(
                            Color(0xFF3B82F6).copy(alpha = 0.0f),
                            Color(0xFF3B82F6)
                        ),
                        startX = size.width+50f,
                        endX = size.width - 45f,
                    )
                    drawOval(
                        brush = brush,
                        topLeft = Offset(SCREEN_RADIUS*2f - 45, -SCREEN_RADIUS * 0.5f),
                        size = Size(SCREEN_RADIUS * 2f, SCREEN_RADIUS * 3f)
                    )
                }

                for (wave in waves) {
                    when (wave.animationStage) {
                        AnimationStage.FIRST, AnimationStage.NOT_STARTED -> {
                            val startX = if(wave.direction > 0) -50f else size.width + 50f
                            val endX = if (wave.direction > 0) max(50f,size.width * wave.animationProgress)
                                       else (size.width - max(50f,size.width * wave.animationProgress))

                            val brush = horizontalGradient(
                                colors = listOf(
                                    wave.color.copy(alpha = 0.0f),
                                    wave.color
                                ),
                                startX = startX,
                                endX = endX,
                            )
                            drawOval(
                                brush = brush,
                                topLeft = wave.topLeft,
                                size = wave.size,
                            )
                        }
                        AnimationStage.SECOND -> {
                            val brush = horizontalGradient(
                                colors = listOf(
                                    wave.color.copy(alpha = 0.0f),
                                    wave.color
                                ),
                                startX = wave.topLeft.x,
                                endX = size.width,
                            )
                            drawOval(
                                brush = brush,
                                topLeft = wave.topLeft,
                                size = wave.size,
                            )
//                            drawOval(
//                                color = wave.color.copy(alpha = 1 - wave.animationProgress),
//                                topLeft = Offset( size.width-50f,wave.topLeft.y),
//                                size = wave.size,
//                            )
                        }
                        AnimationStage.DONE -> { /* nothing to draw */ }
                    }
                }
            }
        }
    }
}
