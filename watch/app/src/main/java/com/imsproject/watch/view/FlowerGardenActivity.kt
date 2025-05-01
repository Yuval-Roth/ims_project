package com.imsproject.watch.view

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import com.imsproject.common.gameserver.GameType
import com.imsproject.watch.BUBBLE_PINK_COLOR
import com.imsproject.watch.DARK_BEIGE_COLOR
import com.imsproject.watch.LIGHT_BACKGROUND_COLOR
import com.imsproject.watch.SCREEN_HEIGHT
import com.imsproject.watch.SCREEN_RADIUS
import com.imsproject.watch.SCREEN_WIDTH
import com.imsproject.watch.WATER_RIPPLES_BUTTON_SIZE
import com.imsproject.watch.utils.polarToCartesian
import com.imsproject.watch.viewmodel.FlowerGardenViewModel
import com.imsproject.watch.viewmodel.GameViewModel
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.sqrt
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

        var sway = remember { mutableFloatStateOf(0f) }
        val swayStep = 0.01f

        val waterDropletsCenters = remember {
            listOf(Pair(SCREEN_HEIGHT/4f, 0.0), Pair(SCREEN_HEIGHT/3f, -50.0),
                Pair(SCREEN_HEIGHT/3f, 50.0), Pair(2*SCREEN_HEIGHT/5f, 22.5),
            Pair(2*SCREEN_HEIGHT/5f, -22.5))
        }

        var dropletAmplitude = remember { List(5) { mutableFloatStateOf(1f) } }
        var plantAmplitude = remember { List(5) { mutableFloatStateOf(1f) } }

        val rng = remember { Random.Default }
        val rng2 = remember { Random.Default }

        val flowerAnimationRadius = remember { mutableFloatStateOf(0f) }

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

                // draw active flowers with animation to the latest
                for ((i, pair) in viewModel.activeFlowerPoints.withIndex()) {
                    val coor = polarToCartesian(pair.first, pair.second)
                    val isLatest = i == viewModel.activeFlowerPoints.lastIndex
                    val radius = if (isLatest) h / 55f + flowerAnimationRadius.floatValue else h / 55f

                    drawCircle(
                        BUBBLE_PINK_COLOR.copy(alpha = 0.8f),
                        radius = radius,
                        style = Fill,
                        center = Offset(x = coor.first, y = coor.second)
                    )
                }

                // draw water droplets - actual water droplets
                if (viewModel.waterDroplet.visible) {
                    for(i in 0..4) {
                        val coor = polarToCartesian(waterDropletsCenters[i].first, -90 + waterDropletsCenters[i].second)
                        val centerX = coor.first
                        val centerY = coor.second + drop.floatValue * dropletAmplitude[i].floatValue

                        waterDropletShape(centerX, centerY, viewModel.waterDroplet.color)
                    }
                }

                // draw plant - shaped like grass
                if (viewModel.plant.visible) {
                    for(i in 0..4) {
                        val coor = polarToCartesian(waterDropletsCenters[i].first, 90 + waterDropletsCenters[i].second)
                        val centerX = coor.first
                        val centerY = coor.second

                        drawGrassStroke(centerX = centerX, centerY = centerY, height = 30f, width = 15f, color = viewModel.plant.color, amplitude = plantAmplitude[i].floatValue * sway.floatValue)
                    }
                }
            }
        }

        val latestFlower = viewModel.activeFlowerPoints.lastOrNull()

        // Only trigger animation when a new flower is added
        LaunchedEffect(latestFlower) {
            latestFlower?.let {
                // pulse size up
                repeat(10) {
                    flowerAnimationRadius.floatValue += 1f
                    delay(16)
                }
                // pulse size down
                repeat(10) {
                    flowerAnimationRadius.floatValue -= 1f
                    delay(16)
                }
                flowerAnimationRadius.floatValue = 0f
            }
        }

        LaunchedEffect(Unit){
            val dropletRngLowerRange = 1f
            val dropletRngUpperRange = 3f
            val grassRngLowerRange = 0.9f
            val grassRngUpperRange = 1.1f

            while(true) {
                // animate water droplets
                if(viewModel.waterDroplet.visible) {
                    val currDropletColor = viewModel.waterDroplet.color

                    // a new click resets the drop animation
                    if(viewModel.freshDropletClick) {
                        viewModel.freshDropletClick = false
                        drop.floatValue = 0f
                        //randomize the extent of the drop for each one
                        for(i in 0..4) {
                            dropletAmplitude[i].floatValue = dropletRngLowerRange + rng.nextFloat() * (dropletRngUpperRange - dropletRngLowerRange)
                        }
                    }
                    // increase the water drop
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

                // animate plant grass
                if(viewModel.plant.visible) {
                    val currPlantColor = viewModel.plant.color

                    // a new click resets the
                    if(viewModel.freshPlantClick) {
                        viewModel.freshPlantClick = false
                        sway.floatValue = 0f
                        //randomize the extent
                        for(i in 0..4) {
                            plantAmplitude[i].floatValue = grassRngLowerRange + rng2.nextFloat() * (grassRngUpperRange - grassRngLowerRange)
                        }
                    }
                    // increase
                    sway.floatValue += swayStep

                    // decrease the opacity
                    val nextAlpha = max(currPlantColor.alpha - 0.01f, 0f)
                    viewModel.plant.color = currPlantColor.copy(nextAlpha)

                    // hide from the screen and reset position
                    if(nextAlpha <= 0f) {
                        viewModel.plant.visible = false
                        sway.floatValue = 0f
                    }
                }
                delay(16)
            }
        }
    }


    fun DrawScope.waterDropletShape(
                          baseX: Float,
                          baseY: Float,
                          color : Color,
                          size: Float = 0.2f,
                          ) {
            val stemHeight = 100f * size

            // Left Part Path
            val leftPart = Path().apply {
                moveTo(baseX, baseY - stemHeight)
                cubicTo(
                    baseX - 80f * size,
                    baseY - stemHeight - 100f * size,
                    baseX - 50f * size,
                    baseY - stemHeight - 150f * size,
                    baseX,
                    baseY - stemHeight - 150f * size
                )
            }

            leftPart.transform(Matrix().apply {
                translate(baseX, baseY - stemHeight)
                scale(1f, -1f)
                translate(-baseX, -(baseY - stemHeight))
            })

            drawPath(leftPart, color, style = Fill)

            // Right Part Path
            val rightPart = Path().apply {
                moveTo(baseX, baseY - stemHeight)
                cubicTo(
                    baseX + 80f * size,
                    baseY - stemHeight - 100f * size,
                    baseX + 50f * size,
                    baseY - stemHeight - 150f * size,
                    baseX,
                    baseY - stemHeight - 150f * size
                )
            }

            rightPart.transform(Matrix().apply {
                translate(baseX, baseY - stemHeight)
                scale(1f, -1f)
                translate(-baseX, -(baseY - stemHeight))
            })

            drawPath(rightPart, color, style = Fill)
    }

    fun DrawScope.drawGrassStroke(centerX: Float, centerY: Float, height: Float, width: Float, color: Color, strokeWidth: Float = 3f, amplitude : Float = 1f) {
        val halfWidth = width / 2f
        val steps = 30

        // Right curve: y = sqrt(x)
        val rightPath = Path()
        for (i in 0..steps) {
            val t = i / steps.toFloat()
            val x = t * halfWidth
            val y = sqrt(t) * height * (1 + amplitude)
            val screenX = centerX + x
            val screenY = centerY - y
            if (i == 0) {
                rightPath.moveTo(screenX, screenY)
            } else {
                rightPath.lineTo(screenX, screenY)
            }
        }

        // Left curve: y = sqrt(-x)
        val leftPath = Path()
        for (i in 0..steps) {
            val t = i / steps.toFloat()
            val x = -t * halfWidth
            val y = sqrt(t) * height * (1 + amplitude - 0.1f)
            val screenX = centerX + x
            val screenY = centerY - y
            if (i == 0) {
                leftPath.moveTo(screenX, screenY)
            } else {
                leftPath.lineTo(screenX, screenY)
            }
        }

        drawPath(rightPath, color, style = Stroke(width = strokeWidth))
        drawPath(leftPath, color, style = Stroke(width = strokeWidth))
    }


    companion object {
        private const val TAG = "WaterRipplesActivity"
    }
}

