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
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import com.imsproject.common.gameserver.GameType
import com.imsproject.watch.DARKER_BROWN_COLOR
import com.imsproject.watch.DARKER_DARKER_BROWN_COLOR
import com.imsproject.watch.DARK_GREEN_BACKGROUND_COLOR
import com.imsproject.watch.GRASS_PLANT_FADE_COEFFICIENT
import com.imsproject.watch.GRASS_PLANT_FADE_THREASHOLD
import com.imsproject.watch.GRASS_PLANT_SWAY_BASE_AMPLITUDE
import com.imsproject.watch.GRASS_PLANT_SWAY_STEP
import com.imsproject.watch.SCREEN_RADIUS
import com.imsproject.watch.WATER_DROPLET_BASE_HEIGHT
import com.imsproject.watch.WATER_DROPLET_BASE_WIDTH
import com.imsproject.watch.WATER_DROPLET_DROP_STEP
import com.imsproject.watch.WATER_DROPLET_FADE_COEFFICIENT
import com.imsproject.watch.WATER_DROPLET_FADE_THREASHOLD
import com.imsproject.watch.WATER_RIPPLES_BUTTON_SIZE
import com.imsproject.watch.viewmodel.FlowerGardenViewModel
import com.imsproject.watch.viewmodel.FlowerGardenViewModel.Flower
import com.imsproject.watch.viewmodel.GameViewModel
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

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
        // this is used only to to trigger recomposition when new ripples are added //todo: can i remove it? UPDATE: NO
        viewModel.counter.collectAsState().value

        // flowers
        val flowerAnimationRadius = remember { mutableFloatStateOf(0f) }
        val shouldAnimateFlower = viewModel.currFlowerIndex.collectAsState().value

        // Box to draw the background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = DARK_GREEN_BACKGROUND_COLOR)
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
                        BorderStroke(2.dp, DARKER_DARKER_BROWN_COLOR),
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
                    backgroundColor = DARKER_BROWN_COLOR,
                    contentColor = Color.Black
                )
            ){}

            Canvas(modifier = Modifier.fillMaxSize()) {
                val h = SCREEN_RADIUS * 2f

                // draw active flowers with animation to the latest
                for ((i, flower) in viewModel.activeFlowerPoints.withIndex()) {
                    val toDrawBigger = i == viewModel._currFlowerIndex.value
                    val radius = if (toDrawBigger) h / 20f + flowerAnimationRadius.floatValue else h / 20f

                    drawFlower(flower, radius = radius)
                }

                // draw water droplets - actual water droplets
                for(waterDropletSet in viewModel.waterDropletSets) {
                    for(center in waterDropletSet.centers) {
                        val centerX = center.first + waterDropletSet.centerXoffset
                        val centerY = center.second + waterDropletSet.centerYoffset + waterDropletSet.drop

                        drawWaterDroplet(centerX, centerY, waterDropletSet.color)
                   }
                }

                // draw plant - shaped like grass
                for(grassPlantSet in viewModel.grassPlantSets) {
                    for((i,center) in grassPlantSet.centers.withIndex()) {
                        val centerX = center.first + grassPlantSet.centerXoffset[i]
                        val centerY = center.second + grassPlantSet.centerYoffset[i]

                        drawGrassStroke(centerX, centerY, WATER_DROPLET_BASE_HEIGHT, WATER_DROPLET_BASE_WIDTH, grassPlantSet.color,
                            GRASS_PLANT_SWAY_BASE_AMPLITUDE * grassPlantSet.sway)
                    }
                }
            }
        }


        // Only trigger animation when a new flower is added
        LaunchedEffect(shouldAnimateFlower) {
            shouldAnimateFlower.let {
                // pulse size up
                repeat(10) {
                    flowerAnimationRadius.floatValue = min(flowerAnimationRadius.floatValue + 1f, 10f)
                    delay(16)
                }
                // pulse size down
                repeat(10) {
                    flowerAnimationRadius.floatValue = max(flowerAnimationRadius.floatValue - 1f, 0f)
                    delay(16)
                }
                flowerAnimationRadius.floatValue = 0f
            }
        }

        LaunchedEffect(Unit){
            while(true) {
                // animate water droplets
                val it = viewModel.waterDropletSets.iterator()
                while (it.hasNext()) {
                    val waterDropletSet = it.next()

                    waterDropletSet.drop += WATER_DROPLET_DROP_STEP // increase the water drop

                    // decrease the opacity
                    val currDropletColor = waterDropletSet.color
                    val nextAlpha = currDropletColor.alpha * exp(WATER_DROPLET_FADE_COEFFICIENT
                            * waterDropletSet.time)
                    waterDropletSet.time++
                    waterDropletSet.color = currDropletColor.copy(nextAlpha)

                    //remove done water droplets
                    if(waterDropletSet.color.alpha <= WATER_DROPLET_FADE_THREASHOLD) {
                        it.remove()
                        continue
                    }
                }

                // animate plant grass
                val it2 = viewModel.grassPlantSets.iterator()
                while (it2.hasNext()) {
                    val grassPlantSet = it2.next()

                    grassPlantSet.sway += GRASS_PLANT_SWAY_STEP
                    // decrease the opacity

                    val currPlantColor = grassPlantSet.color
                    val nextAlpha = currPlantColor.alpha * exp(GRASS_PLANT_FADE_COEFFICIENT
                            * grassPlantSet.time)
                    grassPlantSet.time++
                    grassPlantSet.color = currPlantColor.copy(nextAlpha)

                    //remove done water droplets
                    if(grassPlantSet.color.alpha <= GRASS_PLANT_FADE_THREASHOLD) {
                        it2.remove()
                        continue
                    }
                }
                delay(16)
            }
        }
    }

    fun DrawScope.drawWaterDroplet(baseX: Float, baseY: Float, color: Color, size: Float = 0.23f) {
        val diameter = 100f * size
        val controlOffsetX = 80f * size
        val controlOffsetY = 100f * size
        val tipOffsetY = 150f * size

        val path = Path().apply {
            moveTo(baseX, baseY - diameter) // bottom center
            cubicTo(
                baseX - controlOffsetX, baseY - diameter - controlOffsetY,
                baseX - 50f * size, baseY - diameter - tipOffsetY,
                baseX, baseY - diameter - tipOffsetY
            )
            cubicTo(
                baseX + 50f * size, baseY - diameter - tipOffsetY,
                baseX + controlOffsetX, baseY - diameter - controlOffsetY,
                baseX, baseY - diameter
            )
            close()
        }

        path.transform(Matrix().apply {
            translate(baseX, baseY - diameter)
            scale(1f, -1f)
            translate(-baseX, -(baseY - diameter))
        })

        drawPath(path, color, style = Fill)
    }

    fun DrawScope.drawGrassStroke(centerX: Float, centerY: Float, height: Float, width: Float, color: Color, amplitude : Float = GRASS_PLANT_SWAY_BASE_AMPLITUDE) {
        val strokeWidth = 3f
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

