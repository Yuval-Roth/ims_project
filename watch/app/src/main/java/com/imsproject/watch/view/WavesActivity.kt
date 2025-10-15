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
import androidx.compose.ui.graphics.BlendMode
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
        val wave = remember { viewModel.wave }
        val turn by viewModel.turn.collectAsState()

        LaunchedEffect(wave.direction) {
            if(wave.direction == 0) return@LaunchedEffect

            val mod = if (wave.direction > 0) 1f else -1f
            val progress = Animatable(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = wave.animationLength,
                    easing = OceanWaveEasing,
                )
            ){
                val x = if (wave.direction > 0) {
                    -SCREEN_RADIUS*0.7f + SCREEN_RADIUS * 2.4f * value
                } else {
                    SCREEN_RADIUS * 1.7f - SCREEN_RADIUS * 2.4f * value
                }
                wave.topLeft = Offset( x, 0f)
                wave.animationProgress = value
            }
            viewModel.flipTurn()
        }

//        LaunchedEffect(Unit) {
//            snapshotFlow { viewModel.waves.lastOrNull() }
//                .collect { wave ->
//                    if (wave == null) return@collect
//                    if (wave.animationStage == AnimationStage.NOT_STARTED){
//                        scope.launch(Dispatchers.Main) {
//                            wave.animationStage = AnimationStage.FIRST
//                            val mod = if (wave.direction > 0) 1f else -1f
//                            val progress = Animatable(0f)
//                            progress.animateTo(
//                                targetValue = 1f,
//                                animationSpec = tween(
//                                    durationMillis = wave.animationLength,
//                                    easing = OceanWaveEasing,
//                                )
//                            ){
//                                wave.topLeft = Offset( -mod*(SCREEN_RADIUS * 2) + mod*2*SCREEN_RADIUS*value , -SCREEN_RADIUS * 0.5f)
//                                wave.animationProgress = value
//                            }
//                            wave.animationStage = AnimationStage.SECOND
//                            progress.animateTo(
//                                targetValue = 0.1f,
//                                animationSpec = tween(
//                                    durationMillis = 200,
//                                    easing = LinearEasing,
//                                )
//                            ){
//                                wave.topLeft = Offset( mod*SCREEN_RADIUS * 2 + -mod*2*SCREEN_RADIUS*value , -SCREEN_RADIUS * 0.5f)
//                                wave.animationProgress = value
//                            }
//                            viewModel.flipTurn()
//                            wave.animationStage = AnimationStage.DONE
//                        }
//                    }
//                }
//        }

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
            Canvas(modifier = Modifier
                .fillMaxSize()
                .background(color = Color.White)
            ) {
                if(turn > 0){

                }
                if(turn < 0){

                }
                val brush2 = horizontalGradient(
                    colors = listOf(
                        Color.White,
                        Color(0xBFB5DAFA),
                        Color(0xBFB5DAFA),
                        Color.White,
                    ),
                    startX = wave.topLeft.x + SCREEN_RADIUS*1.15f*0.25f,
                    endX = wave.topLeft.x + SCREEN_RADIUS*1.15f*0.75f,
                )
                drawRect(
                    brush = brush2,
                    topLeft = Offset(wave.topLeft.x+SCREEN_RADIUS*1.15f*0.25f, 0f),
                    size = Size(SCREEN_RADIUS*1.15f*0.5f,SCREEN_RADIUS * 2),
                )
                val brush = horizontalGradient(
                    colors = listOf(
                        Color.White,
                        Color(0xFF6BCBFF),
                        Color.White,
                    ),
                    startX = wave.topLeft.x,
                    endX = wave.topLeft.x + SCREEN_RADIUS*1.15f,
                )
                drawRect(
                    brush = brush,
                    topLeft = wave.topLeft,
                    size = Size(SCREEN_RADIUS*1.15f,SCREEN_RADIUS * 2),
                    blendMode = BlendMode.Multiply
                )
            }
        }
    }
}
