package com.imsproject.watch.view

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.imsproject.watch.GRASS_GREEN_COLOR
import com.imsproject.watch.GRASS_PLANT_BASE_HEIGHT
import com.imsproject.watch.GRASS_PLANT_BASE_WIDTH
import com.imsproject.watch.GRASS_PLANT_STROKE_WIDTH
import com.imsproject.watch.GRASS_WATER_VISIBILITY_THRESHOLD
import com.imsproject.watch.SCREEN_RADIUS
import com.imsproject.watch.WATER_BLUE_COLOR
import com.imsproject.watch.WATER_RIPPLES_BUTTON_SIZE
import com.imsproject.watch.view.contracts.Result
import com.imsproject.watch.viewmodel.FlowerGardenViewModel
import com.imsproject.watch.viewmodel.FlowerGardenViewModel.Flower
import com.imsproject.watch.viewmodel.GameViewModel
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class FlowerGardenActivity : GameActivity(GameType.FLOWER_GARDEN) {

    private val viewModel : FlowerGardenViewModel by viewModels<FlowerGardenViewModel>()

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
            GameViewModel.State.PLAYING -> FlowerGarden(viewModel)
            else -> super.Main()
        }
        super.CheckConnection()
    }

    companion object {
        private const val TAG = "FlowerGardenActivity"
    }
}

@Composable
fun FlowerGarden(viewModel: FlowerGardenViewModel) {
    val scope = rememberCoroutineScope()

    // flowers
    val flowerAnimationRadius = remember { Animatable(0f) }
    val shouldAnimateFlower = viewModel.currFlowerIndex.collectAsState().value

    // sets
    val grassPlantSetsToShow = remember { mutableStateListOf<FlowerGardenViewModel.Plant>() }
    val waterDropletSetsToShow = remember { mutableStateListOf<FlowerGardenViewModel.WaterDroplet>() }
    val flowerPointsToShow = remember { mutableStateListOf<FlowerGardenViewModel.Flower>() }

    LaunchedEffect(Unit){
        while(true){
            awaitFrame()
            viewModel.waterDropletSets.forEach { waterDropletSet ->
                if(waterDropletSet.animationStarted) return@forEach
                waterDropletSet.animationStarted = true

                waterDropletSetsToShow.add(waterDropletSet)
                scope.launch {
                    delay(GRASS_WATER_VISIBILITY_THRESHOLD.toLong())
                    waterDropletSetsToShow.remove(waterDropletSet)
                    viewModel.waterDropletSets.remove(waterDropletSet)
                }
            }
            viewModel.grassPlantSets.forEach { grassPlantSet ->
                if(grassPlantSet.animationStarted) return@forEach
                grassPlantSet.animationStarted = true

                grassPlantSetsToShow.add(grassPlantSet)
                scope.launch {
                    delay(GRASS_WATER_VISIBILITY_THRESHOLD.toLong())
                    grassPlantSetsToShow.remove(grassPlantSet)
                    viewModel.grassPlantSets.remove(grassPlantSet)
                }
            }
            while(viewModel.activeFlowerPoints.isNotEmpty()){
                val flowerPoint = viewModel.activeFlowerPoints.removeFirst()
                flowerPointsToShow.add(flowerPoint)
            }
        }
    }

    // Only trigger animation when a new flower is added
    LaunchedEffect(shouldAnimateFlower) {
        flowerAnimationRadius.animateTo(
            targetValue = 10f,
            animationSpec = tween(
                durationMillis = 150,
                easing = LinearEasing
            )
        )
        flowerAnimationRadius.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = 150,
                easing = LinearEasing
            )
        )
    }


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
        ){
            //draw instruction icon
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    if (viewModel.myItemType == FlowerGardenViewModel.ItemType.WATER) {
                        val centerX = size.width / 2
                        val centerY = size.height / 2
                        drawWaterDroplet(centerX, centerY, WATER_BLUE_COLOR)
                    } else {
                        val centerX = size.width / 2
                        val centerY = size.height / 2 + GRASS_PLANT_BASE_HEIGHT / 2
                        drawGrassStroke(centerX, centerY, GRASS_PLANT_BASE_HEIGHT, GRASS_PLANT_BASE_WIDTH, GRASS_GREEN_COLOR)
                    }
                }
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val h = SCREEN_RADIUS * 2f

            // draw active flowers with animation to the latest
            for ((i, flower) in flowerPointsToShow.withIndex()) {
//                    val toDrawBigger = i == viewModel._currFlowerIndex.value
                val toDrawBigger = i == (flowerPointsToShow.size - 1)

                val radius = if (toDrawBigger) h / 20f + flowerAnimationRadius.value else h / 20f

                drawFlower(flower, radius = radius)
            }

            // draw water droplets - actual water droplets
            for(waterDropletSet in waterDropletSetsToShow) {
                for(center in waterDropletSet.centers) {
                    val centerX = center.first
                    val centerY = center.second

                    drawWaterDroplet(centerX, centerY, waterDropletSet.color)
                }
            }

            // draw plant - shaped like grass
            for(grassPlantSet in grassPlantSetsToShow) {
                for(center in grassPlantSet.centers) {
                    val baseX = center.first
                    val baseY = center.second + GRASS_PLANT_BASE_HEIGHT / 2

                    drawGrassStroke(baseX, baseY, GRASS_PLANT_BASE_HEIGHT, GRASS_PLANT_BASE_WIDTH, grassPlantSet.color,)
                }
            }
        }
    }
}

private fun DrawScope.drawWaterDroplet(baseX: Float, baseY: Float, color: Color, size: Float = 0.23f) {
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

private fun DrawScope.drawGrassStroke(baseX: Float, baseY: Float, height: Float, width: Float, color: Color) {
    val strokeWidth = GRASS_PLANT_STROKE_WIDTH
    val halfWidth = width / 2f
    val steps = 30

    // Right curve: y = sqrt(x)
    val rightPath = Path()
    for (i in 0..steps) {
        val t = i / steps.toFloat()
        val x = t * halfWidth
        val y = sqrt(t) * height
        val screenX = baseX + x
        val screenY = baseY - y
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
        val y = sqrt(t) * height
        val screenX = baseX + x
        val screenY = baseY - y
        if (i == 0) {
            leftPath.moveTo(screenX, screenY)
        } else {
            leftPath.lineTo(screenX, screenY)
        }
    }

    drawPath(rightPath, color, style = Stroke(width = strokeWidth))
    drawPath(leftPath, color, style = Stroke(width = strokeWidth))
}

private fun DrawScope.drawFlower(
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

