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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import com.imsproject.common.gameserver.GameType
import com.imsproject.watch.DARK_BEIGE_COLOR
import com.imsproject.watch.LIGHT_BACKGROUND_COLOR
import com.imsproject.watch.RIPPLE_MAX_SIZE
import com.imsproject.watch.SCREEN_HEIGHT
import com.imsproject.watch.SCREEN_RADIUS
import com.imsproject.watch.SCREEN_WIDTH
import com.imsproject.watch.WATER_RIPPLES_BUTTON_SIZE
import com.imsproject.watch.WHITE_ANTIQUE_COLOR
import com.imsproject.watch.utils.polarToCartesian
import com.imsproject.watch.viewmodel.FlowerGardenViewModel
import com.imsproject.watch.viewmodel.FlowerGardenViewModel.Flower
import com.imsproject.watch.viewmodel.GameViewModel
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
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
        // this is used only to to trigger recomposition when new ripples are added //todo: can i remove it?
        viewModel.counter.collectAsState().value

        //shared
        val waterAndPlantCenters = remember {
            listOf(Pair(SCREEN_HEIGHT/3.5f, 30.0), Pair(SCREEN_HEIGHT/3.5f, -30.0),
                Pair(SCREEN_HEIGHT/3.5f, 60.0), Pair(SCREEN_HEIGHT/3.5f, -60.0), Pair(SCREEN_HEIGHT/3.5f, 0.0)) }
        val numOfUnits = waterAndPlantCenters.size
        val rng = remember { Random.Default }

        // water droplets
        val dropStep = 0.8f

        // plants (grass)
        var sway = remember { mutableFloatStateOf(0f) }
        val swayStep = 0.01f
        var plantAmplitude = remember { List(5) { mutableFloatStateOf(1f) } }

        // flowers
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
                onClick = {},
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = DARK_BEIGE_COLOR,
                    contentColor = Color.Black
                )
            ){}

            Canvas(modifier = Modifier.fillMaxSize()) {
                val h = SCREEN_HEIGHT

                // draw active flowers with animation to the latest
                for ((i, flower) in viewModel.activeFlowerPoints.withIndex()) {
                    val isLatest = i == viewModel.activeFlowerPoints.lastIndex
                    val radius = if (isLatest) h / 30f + flowerAnimationRadius.floatValue else h / 30f

                    drawFlower(flower, radius = radius)
                }

                // draw water droplets - actual water droplets
                for(waterDropletSet in viewModel.waterDropletSets) {
                    for(center in waterDropletSet.centers) {
                        val centerX = center.first
                        val centerY = center.second + waterDropletSet.drop

                        drawWaterDroplet(centerX, centerY, waterDropletSet.color)
                   }
                }

                // draw plant - shaped like grass
                if (viewModel.plant.visible) {
                    for(i in 0 until numOfUnits) {
                        val coor = polarToCartesian(waterAndPlantCenters[i].first, 90 + waterAndPlantCenters[i].second)
                        val centerX = coor.first
                        val centerY = coor.second

                        drawGrassStroke(centerX = centerX, centerY = centerY, height = 20f, width = 10f, color = viewModel.plant.color, amplitude = plantAmplitude[i].floatValue * sway.floatValue)
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
            val grassRngLowerRange = 0.9f
            val grassRngUpperRange = 1.1f

            while(true) {
                // animate water droplets
                val it = viewModel.waterDropletSets.iterator()
                while (it.hasNext()) {
                    val waterDropletSet = it.next()

                    waterDropletSet.drop += dropStep // increase the water drop
                    // decrease the opacity

                    val currDropletColor = waterDropletSet.color
                    val nextAlpha = max(currDropletColor.alpha - 0.03f, 0f)
                    waterDropletSet.color = currDropletColor.copy(nextAlpha)

                    //remove done water droplets
                    if(waterDropletSet.color.alpha <= 0f) {
                        it.remove()
                        continue
                    }
                }

                // animate plant grass
                if(viewModel.plant.visible) {
                    val currPlantColor = viewModel.plant.color

                    // a new click resets the sway
                    if(viewModel.freshPlantClick) {
                        viewModel.freshPlantClick = false
                        sway.floatValue = 0f
                        //randomize the extent
                        for(i in 0 until numOfUnits) {
                            plantAmplitude[i].floatValue = grassRngLowerRange + rng.nextFloat() * (grassRngUpperRange - grassRngLowerRange)
                        }
                    }
                    // increase sway
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


    fun DrawScope.drawWaterDroplet(baseX: Float, baseY: Float, color : Color, size: Float = 0.15f) {
            val diameter = 100f * size

            // Left Part Path
            val leftPart = Path().apply {
                moveTo(baseX, baseY - diameter)
                cubicTo(baseX - 80f * size, baseY - diameter - 100f * size, baseX - 50f * size,
                    baseY - diameter - 150f * size, baseX, baseY - diameter - 150f * size)
            }

            leftPart.transform(Matrix().apply {
                translate(baseX, baseY - diameter)
                scale(1f, -1f)
                translate(-baseX, -(baseY - diameter))
            })

            drawPath(leftPart, color, style = Fill)

            // Right Part Path
            val rightPart = Path().apply {
                moveTo(baseX, baseY - diameter)
                cubicTo(
                    baseX + 80f * size, baseY - diameter - 100f * size, baseX + 50f * size,
                    baseY - diameter - 150f * size, baseX, baseY - diameter - 150f * size
                )
            }

            rightPart.transform(Matrix().apply {
                translate(baseX, baseY - diameter)
                scale(1f, -1f)
                translate(-baseX, -(baseY - diameter))
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

    fun DrawScope.drawFlower(
        flower : Flower,
        radius: Float,
    ) {
        val petalLength: Float = flower.petalHeightCoef * radius
        val petalWidth: Float = flower.petalWidthCoef * radius
        val angleStep = 360f / flower.numOfPetals

        for (i in 0 until flower.numOfPetals) {
            val angleDeg = angleStep * i
            val angleRad = Math.toRadians(angleDeg.toDouble())

            val petalCenterX = flower.centerX + (radius * 0.6f * cos(angleRad)).toFloat()
            val petalCenterY = flower.centerY + (radius * 0.6f * sin(angleRad)).toFloat()

            val petalCenter = Offset(petalCenterX, petalCenterY)

            rotate(angleDeg, pivot = petalCenter) {
                drawOval(
                    color = flower.petalColor,
                    topLeft = Offset(
                        x = petalCenter.x - petalLength / 2,
                        y = petalCenter.y - petalWidth / 2
                    ),
                    size = Size(width = petalLength, height = petalWidth)
                )
            }
        }

        // Center of the flower
        drawCircle(
            color = flower.centerColor,
            radius = radius * 0.3f,
            center = Offset(flower.centerX, flower.centerY)
        )
    }

    companion object {
        private const val TAG = "WaterRipplesActivity"
    }
}

