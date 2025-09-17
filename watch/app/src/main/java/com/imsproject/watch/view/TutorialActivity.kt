package com.imsproject.watch.view

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.imsproject.common.gameserver.GameType
import com.imsproject.watch.WATER_RIPPLES_BUTTON_SIZE
import com.imsproject.watch.viewmodel.FlourMillViewModel
import com.imsproject.watch.viewmodel.FlowerGardenViewModel
import com.imsproject.watch.viewmodel.TutorialViewModel
import com.imsproject.watch.viewmodel.WaterRipplesViewModel
import com.imsproject.watch.viewmodel.WineGlassesViewModel
import kotlinx.coroutines.delay
import kotlin.getValue

class TutorialActivity: GameActivity(GameType.TUTORIAL) {
    private val tutorialViewModel by viewModels<TutorialViewModel>()
    private val waterRipplesViewModel by viewModels<WaterRipplesViewModel>()
    private val wineGlassesViewModel by viewModels<WineGlassesViewModel>()
    private val flourMillViewModel by viewModels<FlourMillViewModel>()
    private val flowerGardenViewModel by viewModels<FlowerGardenViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.onCreate(tutorialViewModel)
        waterRipplesViewModel.tutorialMode()
        waterRipplesViewModel.onCreate(intent, applicationContext)
//        wineGlassesViewModel.tutorialMode()
//        flourMillViewModel.tutorialMode()
//        flowerGardenViewModel.tutorialMode()
        setContent {
            Main()
        }
    }

    @Composable
    override fun Main(){
        val currentTutorial = tutorialViewModel.currentTutorial.collectAsState().value
        when (currentTutorial) {
            GameType.WATER_RIPPLES -> WaterRipplesTutorial(waterRipplesViewModel) { tutorialViewModel.nextTutorial() }
            GameType.WINE_GLASSES -> WineGlassesTutorial(wineGlassesViewModel) { tutorialViewModel.nextTutorial() }
            GameType.FLOUR_MILL -> FlourMillTutorial(flourMillViewModel) { tutorialViewModel.nextTutorial() }
            GameType.FLOWER_GARDEN -> FlowerGardenTutorial(flowerGardenViewModel){ tutorialViewModel.nextTutorial() }
            else -> throw IllegalStateException("Invalid tutorial game type: $currentTutorial")
        }
    }

    @Composable
    fun WaterRipplesTutorial(viewModel: WaterRipplesViewModel, onNext: () -> Unit) {
        var pause by remember { mutableStateOf(false) }
        var clicked by remember { mutableStateOf(false) }

        LaunchedEffect(Unit){
            pause = true
            while(! clicked){
                delay(100)
            }
            viewModel.click()
            delay(1000)

            repeat(3){
                var clicks = 0
                clicked = false
                delay(500)
                while(clicks < 3){
                    viewModel.tutorialOpponentClick()
                    clicks++
                    delay(500)
                }
                pause = true
                while(! clicked){
                    delay(100)
                }
                viewModel.click()
                viewModel.tutorialOpponentClick()
            }

            while(true){
                viewModel.tutorialOpponentClick()
                delay(1000)
            }
        }

        if (pause){
            WaterRipples(viewModel, true)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = Color.Black.copy(alpha = 0.5f))
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.type == PointerEventType.Press) {
                                    event.changes[0].consume()
                                    pause = false
                                    clicked = true
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                ClickPromptOnButton()
            }
        } else {
            WaterRipples(viewModel, false)
        }
    }

    @Composable
    fun FlowerGardenTutorial(viewModel: FlowerGardenViewModel, onNext: () -> Unit) {
        TODO("Not yet implemented")
    }

    @Composable
    fun FlourMillTutorial(viewModel: FlourMillViewModel, onNext: () -> Unit) {
        TODO("Not yet implemented")
    }

    @Composable
    fun WineGlassesTutorial(viewModel: WineGlassesViewModel, onNext: () -> Unit) {
        TODO("Not yet implemented")
    }

    @Composable
    private fun ClickPromptOnButton() {
        val neon = Color(0xFF00FF57)

        val pulse = rememberInfiniteTransition(label = "pulse")
        val ringScale by pulse.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ringScale"
        )
        val glowAlpha by pulse.animateFloat(
            initialValue = 0.85f,
            targetValue = 0.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowAlpha"
        )

        Box(contentAlignment = Alignment.Center) {
            // Outer glowing ring that gently scales
            Box(
                modifier = Modifier
                    .size(WATER_RIPPLES_BUTTON_SIZE.dp)
                    .graphicsLayer {
                        scaleX = ringScale
                        scaleY = ringScale
                    }
                    .shadow(
                        elevation = 32.dp,
                        shape = CircleShape,
                        spotColor = neon.copy(alpha = glowAlpha)
                    )
                    .border(BorderStroke(3.dp, neon), CircleShape)
                    .clip(CircleShape)
            )

            // Inner solid disc for readability (subtle)
            Box(
                modifier = Modifier
                    .size((WATER_RIPPLES_BUTTON_SIZE * 0.82f).dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(neon.copy(alpha = 0.20f), Color.Transparent)
                        )
                    )
            )

            // The “CLICK” label
            Text(
                text = "לחצו",
                color = neon,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 2.dp) // slight vertical optically centered tweak
            )
        }
    }

}