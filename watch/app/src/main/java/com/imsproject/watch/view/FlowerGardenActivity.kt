package com.imsproject.watch.view

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.lifecycle.viewModelScope
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import com.imsproject.common.gameserver.GameType
import com.imsproject.common.utils.Angle
import com.imsproject.watch.BUBBLE_PINK_COLOR
import com.imsproject.watch.DARK_BACKGROUND_COLOR
import com.imsproject.watch.DARK_BEIGE_COLOR
import com.imsproject.watch.LIGHT_BACKGROUND_COLOR
import com.imsproject.watch.LIGHT_GRAY_COLOR
import com.imsproject.watch.R
import com.imsproject.watch.RIPPLE_MAX_SIZE
import com.imsproject.watch.SCREEN_HEIGHT
import com.imsproject.watch.SCREEN_RADIUS
import com.imsproject.watch.SCREEN_WIDTH
import com.imsproject.watch.WATER_RIPPLES_BUTTON_SIZE
import com.imsproject.watch.initProperties
import com.imsproject.watch.utils.polarToCartesian
import com.imsproject.watch.viewmodel.FlowerGardenViewModel
import com.imsproject.watch.viewmodel.GameViewModel
import com.imsproject.watch.viewmodel.WaterRipplesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

class FlowerGardenActivity : GameActivity(GameType.FLOWER_GARDEN) {

    private val viewModel : FlowerGardenViewModel by viewModels<FlowerGardenViewModel>()
    private var clickSoundId : Int = -1

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
            GameViewModel.State.PLAYING -> FlowerGarden()
            else -> super.Main()
        }
    }

    @SuppressLint("ReturnFromAwaitPointerEventScope")
    @Composable
    fun FlowerGarden() {
        // this is used only to to trigger recomposition when new ripples are added
        viewModel.counter.collectAsState().value

        var drop = remember { mutableFloatStateOf(0f) }
        val step = 0.1f

        val waterDropletsCenters = remember {
            listOf(Pair(SCREEN_HEIGHT/4f, 0.0), Pair(SCREEN_HEIGHT/3f, -50.0),
                Pair(SCREEN_HEIGHT/3f, 50.0), Pair(2*SCREEN_HEIGHT/5f, 22.5),
            Pair(2*SCREEN_HEIGHT/5f, -22.5))
        }

        var amplitude = remember { List(5) { mutableFloatStateOf(1f) } }
        val rng = remember { Random.Default }

        val animatedRadius = remember { mutableFloatStateOf(0f) }
        val flowerAnimationIndex = remember {mutableIntStateOf(0)}

        // Box to draw the background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = LIGHT_BACKGROUND_COLOR)
                .shadow(
                    elevation = (SCREEN_RADIUS * 0.35f).dp,
                    CircleShape,
                    spotColor = Color.Cyan.copy(alpha = 0.5f)
                )
            ,
            contentAlignment = Alignment.Center
        ) {

            // Center button
            Button(
                modifier = Modifier
                    .border(
                        BorderStroke(2.dp, DARK_BEIGE_COLOR),
                        CircleShape
                    )
                    .size(WATER_RIPPLES_BUTTON_SIZE.dp)

                    // handle clicks on the center button
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.type == PointerEventType.Press) {
                                    event.changes[0].consume()
                                    viewModel.click()
                                }
                            }
                        }
                    },
                onClick = {}, // no-op, handled by pointerInput.
                              // We want the action to be on-touch and not on-release
                              // and the onClick callback is called on-release
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = DARK_BEIGE_COLOR,
                    contentColor = Color.Black
                )
            ){
                // empty button content
            }

            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val w = SCREEN_WIDTH
                val h = SCREEN_HEIGHT
                val centerX = w/2f
                val centerY = h/2f

                // draw active flowers with animation to the latest
                for ((i, pair) in viewModel.activeFlowerPoints.withIndex()) {
                    val coor = polarToCartesian(pair.first, pair.second)
                    val isLatest = i == viewModel.activeFlowerPoints.lastIndex
                    val radius = if (isLatest) h / 55f + animatedRadius.floatValue else h / 55f

                    drawCircle(
                        BUBBLE_PINK_COLOR.copy(alpha = 0.8f),
                        radius = radius,
                        style = Fill,
                        center = Offset(x = coor.first, y = coor.second)
                    )
                }

                // draw water droplets
                if (viewModel.waterDroplet.visible) {
                    val radius = h/30f
                    val ovalWidth = radius * 1.3f
                    val ovalHeight = radius * 1.8f // makes it droplet-shaped

                    for(i in 0..4) {
                        val coor = polarToCartesian(waterDropletsCenters[i].first, -90 + waterDropletsCenters[i].second)
                        drawOval(
                            color = viewModel.waterDroplet.color,
                            topLeft = Offset(
                                x = coor.first - ovalWidth / 2f,
                                y = coor.second - ovalHeight / 2f + drop.floatValue * amplitude[i].floatValue
                            ),
                            size = Size(ovalWidth, ovalHeight),
                            style = Fill
                        )
                    }
                }

                if (viewModel.plant.visible) {
                    drawCircle(
                        viewModel.plant.color,
                        radius = h / 5f,
                        style = Fill,
                        center = Offset(x = centerX , y = centerY + 4*h/7)
                    )
                }
            }
        }

        val latestFlower = viewModel.activeFlowerPoints.lastOrNull()

        // Only trigger animation when a new flower is added
        LaunchedEffect(latestFlower) {
            latestFlower?.let {
                // pulse size up
                repeat(10) {
                    animatedRadius.floatValue += 1f
                    delay(16)
                }
                // pulse size down
                repeat(10) {
                    animatedRadius.floatValue -= 1f
                    delay(16)
                }
                animatedRadius.floatValue = 0f
            }
        }

        LaunchedEffect(Unit){
            val a = 1f
            val b = 3f

            while(true) {
                if(viewModel.waterDroplet.visible) {
                    val currDropletColor = viewModel.waterDroplet.color

                    // if click was reset, reset the drop value as well
                    if(currDropletColor.alpha == 1f) {
                        drop.floatValue = 0f
                        //randomize the extent of the drop for each one
                        for(i in 0..4) {
                            amplitude[i].floatValue = a + rng.nextFloat() * (b - a)
                        }
                    }
                    // increase the drop
                    drop.floatValue += step

                    // decrease the opacity
                    val nextAlpha = max(currDropletColor.alpha - 0.01f, 0f)
                    viewModel.waterDroplet.color = currDropletColor.copy(nextAlpha)

                    // hide from the screen and reset position
                    if(nextAlpha <= 0f) {
                        viewModel.waterDroplet.visible = false
                        drop.floatValue = 0f
                    }
                }

                if(viewModel.plant.visible) {
                    val nextAlpha = max(viewModel.plant.color.alpha - 0.01f, 0f)
                    viewModel.plant.color = viewModel.plant.color.copy(nextAlpha)
                    if(nextAlpha <= 0)
                        viewModel.plant.visible = false
                }

                delay(16)
            }
        }
    }

    companion object {
        private const val TAG = "WaterRipplesActivity"
    }
}

