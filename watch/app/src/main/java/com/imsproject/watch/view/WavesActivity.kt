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
import kotlinx.coroutines.Dispatchers
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

        LaunchedEffect(Unit) {
            snapshotFlow { viewModel.waves.lastOrNull() }
                .collect { wave ->
                    if (wave == null) return@collect
                    if (! wave.animationStarted){
                        wave.animationStarted = true
                        scope.launch(Dispatchers.Main) {
                            val mod = if (wave.direction > 0) 1f else -1f
                            val progress = Animatable(0f)
                            progress.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(
                                    durationMillis = wave.animationLength,
                                    easing = OceanWaveEasing,
                                )
                            ){
                                wave.topLeft = Offset( -mod*SCREEN_RADIUS * 2 + mod*2*SCREEN_RADIUS*value , -SCREEN_RADIUS * 0.5f)
                                wave.animationState = value
                            }
                            viewModel.flipTurn()
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
                for (wave in waves) {
                    drawOval(
                        color = wave.color,
                        topLeft = wave.topLeft,
                        size = Size(SCREEN_RADIUS * 2f, SCREEN_RADIUS * 3f),
                    )
                }
            }
        }
    }
}
