package com.imsproject.watch.view

import android.os.Bundle
import android.os.VibrationEffect
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.imsproject.common.gameserver.GameType
import com.imsproject.watch.BLUE_COLOR
import com.imsproject.watch.GRASS_GREEN_COLOR
import com.imsproject.watch.R
import com.imsproject.watch.SCREEN_CENTER
import com.imsproject.watch.SCREEN_RADIUS
import com.imsproject.watch.TREE_PARTICLE_CAGE_STROKE_WIDTH
import com.imsproject.watch.TREE_PARTICLE_RADIUS
import com.imsproject.watch.TREE_RING_ANGLE_STEP
import com.imsproject.watch.TREE_RING_OPENING_ANGLE
import com.imsproject.watch.TREE_RING_ROTATION_DURATION
import com.imsproject.watch.TREE_RING_START_ANGLE
import com.imsproject.watch.TREE_RING_SWEEP_ANGLE
import com.imsproject.watch.utils.FlingTracker
import com.imsproject.watch.utils.cartesianToPolar
import com.imsproject.watch.utils.closestQuantizedAngle
import com.imsproject.watch.utils.quantizeAngles
import com.imsproject.watch.viewmodel.GameViewModel
import com.imsproject.watch.viewmodel.TreeViewModel.ParticleState
import com.imsproject.watch.viewmodel.TreeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import androidx.core.graphics.scale
import com.imsproject.watch.TREE_PARTICLE_RELATIVE_DISTANCE_FROM_CENTER
import com.imsproject.watch.TREE_RING_RADIUS
import com.imsproject.watch.TREE_RING_RELATIVE_RADIUS
import com.imsproject.watch.utils.random
import com.imsproject.watch.view.contracts.Result

class TreeActivity: GameActivity(GameType.TREE) {

    val viewModel: TreeViewModel by viewModels<TreeViewModel>()

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
            GameViewModel.State.PLAYING -> Tree(viewModel)
            else -> super.Main()
        }
        super.CheckConnection()
    }
}

@Composable
fun Tree(viewModel: TreeViewModel) {
    val animateRing by viewModel.animateRing.collectAsState()
    val showLeftSide by viewModel.showLeftSide.collectAsState()
    val showRightSide by viewModel.showRightSide.collectAsState()
    val ringAngle = viewModel.ringAngle.collectAsState().value
    val density = LocalDensity.current.density
    val tracker = remember { FlingTracker() }
    val scope = rememberCoroutineScope()
    val myParticle by viewModel.myParticle.collectAsState()
    val otherParticle by viewModel.otherParticle.collectAsState()
    var leftFlingSuccess by remember { mutableStateOf(false) }
    val leftAuraAlpha = remember { Animatable(0f) }
    var rightFlingSuccess by remember { mutableStateOf(false) }
    val rightAuraAlpha = remember { Animatable(0f) }
    var leafIndex = remember { 0 }
    var bitmapsLoaded = remember { false }
    var leavesBitmaps = remember<Array<ImageBitmap>> { arrayOf() }
    var treeBitmap = remember { ImageBitmap(1, 1) }
    if (!bitmapsLoaded) {
        leavesBitmaps = arrayOf(
            ImageBitmap.imageResource(id = R.drawable.tree_leaves1),
            ImageBitmap.imageResource(id = R.drawable.tree_leaves2),
            ImageBitmap.imageResource(id = R.drawable.tree_leaves3),
            ImageBitmap.imageResource(id = R.drawable.tree_leaves4),
            ImageBitmap.imageResource(id = R.drawable.tree_leaves5),
            ImageBitmap.imageResource(id = R.drawable.tree_leaves6)
        )
            .map { original -> // scale them down
                // Scale each bitmap down to (for example) 50% of its original size
                val scaledWidth = (original.width * 0.08f).toInt()
                val scaledHeight = (original.height * 0.08f).toInt()
                original.asAndroidBitmap().scale(scaledWidth, scaledHeight).asImageBitmap()
            }.toTypedArray()
        treeBitmap = ImageBitmap.imageResource(id = R.drawable.tree).asAndroidBitmap().scale(
            (SCREEN_RADIUS * 0.5f).toInt(),
            (SCREEN_RADIUS * 0.5f).toInt()
        ).asImageBitmap()
        bitmapsLoaded = true
    }
    val leaves =
        remember { mutableListOf<Triple<Offset, ImageBitmap, Animatable<Float, AnimationVector1D>>>() }
    val animateAuraAlpha = remember(Unit) {
        { successAnimation: Animatable<Float, AnimationVector1D>, targetValue: Float ->
            scope.launch {
                successAnimation.animateTo(
                    targetValue = targetValue,
                    animationSpec = tween(
                        durationMillis = 150,
                        easing = LinearEasing
                    )
                )
            }
        }
    }

    LaunchedEffect(animateRing) {
        if (!animateRing) return@LaunchedEffect
        val ringAngle = viewModel.ringAngle
        // rotate ring and check for success events
        val quantizedAngles = quantizeAngles(TREE_RING_ANGLE_STEP)
        val leftThreshold = closestQuantizedAngle(
            180f + TREE_RING_OPENING_ANGLE + TREE_RING_ANGLE_STEP,
            TREE_RING_ANGLE_STEP,
            quantizedAngles
        )
        val rightThreshold = closestQuantizedAngle(
            TREE_RING_OPENING_ANGLE + TREE_RING_ANGLE_STEP,
            TREE_RING_ANGLE_STEP,
            quantizedAngles
        )
        while (true) {
            val currentGameTime = viewModel.getCurrentGameTime()
            val rotationTimePassed = currentGameTime % TREE_RING_ROTATION_DURATION
            val rotationPercentage = rotationTimePassed / TREE_RING_ROTATION_DURATION
            val targetAngle = 360f * rotationPercentage
            val quantizedTargetAngle =
                closestQuantizedAngle(targetAngle, TREE_RING_ANGLE_STEP, quantizedAngles)
            ringAngle.value = quantizedTargetAngle

            // reset success auras at the thresholds
            when (quantizedTargetAngle) {
                leftThreshold -> {
                    if (rightFlingSuccess && !leftFlingSuccess) {
                        animateAuraAlpha(rightAuraAlpha, 0f)
                        rightFlingSuccess = false
                    }
                }

                rightThreshold -> {
                    if (leftFlingSuccess && !rightFlingSuccess) {
                        animateAuraAlpha(leftAuraAlpha, 0f)
                        leftFlingSuccess = false
                    }
                }

                else -> {}
            }
            awaitFrame()
        }
    }

    // animate new particles
    LaunchedEffect(Unit) {
        while (true) {
            for (particle in listOfNotNull(myParticle, otherParticle)) {
                when (particle.state) {
                    ParticleState.NEW -> {
                        particle.state = ParticleState.STATIONARY
                        val alphaAnimation = Animatable(0f)
                        scope.launch {
                            alphaAnimation.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(
                                    durationMillis = 150,
                                    easing = LinearEasing
                                )
                            ) {
                                particle.alpha = value
                            }
                        }
                    }

                    ParticleState.STATIONARY -> {
                        if (particle.animationLength > 0) {
                            particle.state = ParticleState.MOVING

                            val targetX = SCREEN_CENTER.x - TREE_PARTICLE_RADIUS
                            val startX = particle.center.x
                            val distance = targetX - startX
                            val positionAnimation = Animatable(0f)
                            val targetValue = if (particle.success) 1f else ((TREE_PARTICLE_RELATIVE_DISTANCE_FROM_CENTER - TREE_RING_RELATIVE_RADIUS) / TREE_PARTICLE_RELATIVE_DISTANCE_FROM_CENTER)
                            scope.launch {
                                positionAnimation.animateTo(
                                    targetValue = targetValue,
                                    animationSpec = tween(
                                        durationMillis = particle.animationLength,
                                        easing = LinearEasing
                                    )
                                ) {
                                    val newX = startX + distance * value
                                    particle.center = Offset(newX, particle.center.y)
                                }

                                if (particle.success) {
                                    val otherFlingSuccess: Boolean
                                    val thisAuraAlpha: Animatable<Float, AnimationVector1D>
                                    val otherAuraAlpha: Animatable<Float, AnimationVector1D>
                                    when (particle.direction) {
                                        1 -> {
                                            leftFlingSuccess = true
                                            otherFlingSuccess = rightFlingSuccess
                                            thisAuraAlpha = leftAuraAlpha
                                            otherAuraAlpha = rightAuraAlpha
                                        }
                                        -1 -> {
                                            rightFlingSuccess = true
                                            otherFlingSuccess = leftFlingSuccess
                                            thisAuraAlpha = rightAuraAlpha
                                            otherAuraAlpha = leftAuraAlpha
                                        }

                                        else -> {
                                            throw IllegalStateException("Invalid particle direction: ${particle.direction}")
                                        }
                                    }
                                    animateAuraAlpha(thisAuraAlpha, 1f)
                                    if (otherFlingSuccess) {
                                        // both sides succeeded
                                        scope.launch(Dispatchers.Default) {
                                            delay(particle.animationLength - 250L)
                                            viewModel.playRewardSound()
                                        }
                                        val image = leavesBitmaps[leafIndex]
                                        leafIndex = (leafIndex + 1) % leavesBitmaps.size
                                        // random position around the tree
                                        val topLeft = Offset(
                                            x = SCREEN_CENTER.x + (-SCREEN_RADIUS * 0.15f..SCREEN_RADIUS * 0.15f).random() - image.width / 2f,
                                            y = SCREEN_CENTER.y - (-SCREEN_RADIUS * 0.05f..SCREEN_RADIUS * 0.175f).random() - image.height / 2f
                                        )
                                        val sizeAnimation = Animatable(0f)
                                        leaves.add(Triple(topLeft, image, sizeAnimation))
                                        scope.launch {
                                            scope.launch {
                                                delay(500L)
                                                animateAuraAlpha(thisAuraAlpha, 0f)
                                                animateAuraAlpha(otherAuraAlpha, 0f)
                                            }
                                            viewModel.vibrateClick()
                                            sizeAnimation.animateTo(
                                                targetValue = 1f,
                                                animationSpec = spring(
                                                    dampingRatio = 0.4f,
                                                    stiffness = Spring.StiffnessLow
                                                )
                                            )
                                            leftFlingSuccess = false
                                            rightFlingSuccess = false
                                        }
                                    } else {
                                        // only one side succeeded
                                        animateAuraAlpha(thisAuraAlpha, 1f)
                                    }
                                }

                                // after animation ends, reset particle
                                viewModel.resetParticle(particle.direction)
                            }
                        }
                    }

                    ParticleState.MOVING -> { /* do nothing, animation is already running */
                    }
                }
            }
            awaitFrame()
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        val x = startOffset.x
                        val y = startOffset.y
                        val (distance, _) = cartesianToPolar(x, y)
                        val direction = if (x < SCREEN_CENTER.x) 1 else -1
                        if (viewModel.myDirection == direction && distance > SCREEN_RADIUS * 0.6) {
                            tracker.startFling(x, y)
                        }
                    },
                    onDrag = { change: PointerInputChange, _ ->
                        val position = change.position
                        tracker.setOffset(position.x, position.y)
                    },
                    onDragEnd = {
                        if (tracker.started) {
                            val (nx, ny, speedPxPerSec) = tracker.endFling()
                            if (viewModel.myDirection * nx > 0) { // fling in my direction
                                val dpPecSec = speedPxPerSec / density
                                viewModel.fling(dpPecSec)
                            }
                        }
                    },
                    onDragCancel = { }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {

            // draw cages for the particles
            val WALLS_UPPER_Y = SCREEN_CENTER.y - SCREEN_RADIUS * 0.15f
            val WALLS_LOWER_Y = SCREEN_CENTER.y + SCREEN_RADIUS * 0.15f
            val WALL_DISTANCE_FROM_EDGE = SCREEN_RADIUS * 0.06f
            // left side
            if (showLeftSide) {
                val LEFT_INNER_X = SCREEN_CENTER.x - SCREEN_RADIUS * 0.72f
                val path = Path().apply {
                    // upper line

                    moveTo(WALL_DISTANCE_FROM_EDGE, WALLS_UPPER_Y)
                    lineTo(LEFT_INNER_X, WALLS_UPPER_Y)

                    // lower line
                    moveTo(WALL_DISTANCE_FROM_EDGE, WALLS_LOWER_Y)
                    lineTo(LEFT_INNER_X, WALLS_LOWER_Y)

                    addArc(
                        oval = Rect(
                            center = SCREEN_CENTER,
                            radius = SCREEN_RADIUS * 0.96f
                        ),
                        startAngleDegrees = -171f,
                        sweepAngleDegrees = -18f,
                    )
                }
                drawPath(
                    path = path,
                    color = BLUE_COLOR,
                    style = Stroke(
                        width = TREE_PARTICLE_CAGE_STROKE_WIDTH,
                        cap = StrokeCap.Round
                    )
                )
            }
            // right side
            if (showRightSide) {
                val RIGHT_INNER_X = SCREEN_CENTER.x + SCREEN_RADIUS * 0.72f
                val path = Path().apply {
                    // upper right line
                    moveTo(RIGHT_INNER_X, WALLS_UPPER_Y)
                    lineTo(size.width - WALL_DISTANCE_FROM_EDGE, WALLS_UPPER_Y)

                    // lower right line
                    moveTo(RIGHT_INNER_X, WALLS_LOWER_Y)
                    lineTo(size.width - WALL_DISTANCE_FROM_EDGE, WALLS_LOWER_Y)

                    // optional arc (if you want to match symmetry)
                    addArc(
                        oval = Rect(
                            center = SCREEN_CENTER,
                            radius = SCREEN_RADIUS * 0.96f
                        ),
                        startAngleDegrees = -9f,
                        sweepAngleDegrees = 18f
                    )
                }
                drawPath(
                    path = path,
                    color = GRASS_GREEN_COLOR,
                    style = Stroke(
                        width = TREE_PARTICLE_CAGE_STROKE_WIDTH,
                        cap = StrokeCap.Round
                    )
                )
            }
            rotate(degrees = ringAngle) {
                drawRing(
                    center = SCREEN_CENTER,
                    radius = TREE_RING_RADIUS
                )
            }

            // --- draw success auras ---
            val leftColor = Color(0xffe0ebfb)   // blueish
            val rightColor = Color(0xfffcf4db)  // yellowish
            val radius = SCREEN_RADIUS * 0.3f

            val leftAlpha = leftAuraAlpha.value
            val rightAlpha = rightAuraAlpha.value

            when {
                // only left aura active
                leftAlpha > 0f && rightAlpha <= 0f -> {
                    drawCircle(
                        color = leftColor,
                        radius = radius,
                        center = SCREEN_CENTER,
                        alpha = leftAlpha
                    )
                }

                // only right aura active
                rightAlpha > 0f && leftAlpha <= 0f -> {
                    drawCircle(
                        color = rightColor,
                        radius = radius,
                        center = SCREEN_CENTER,
                        alpha = rightAlpha
                    )
                }

                // both active → gradient blend
                leftAlpha > 0f && rightAlpha > 0f -> {
                    val avgAlpha = (leftAlpha + rightAlpha) / 2f

                    // --- if you prefer horizontal transition (blue → yellow left→right), use this instead ---
                    val brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFBFD8FF), Color(0xFFFFE89F)),
                        start = Offset(SCREEN_CENTER.x - radius, SCREEN_CENTER.y),
                        end = Offset(SCREEN_CENTER.x + radius, SCREEN_CENTER.y)
                    )

                    drawCircle(
                        brush = brush,
                        radius = radius,
                        center = SCREEN_CENTER,
                        alpha = avgAlpha
                    )
                }
            }


            // draw tree
            drawImage(
                image = treeBitmap,
                topLeft = Offset(
                    x = SCREEN_CENTER.x - treeBitmap.width / 2f,
                    y = SCREEN_CENTER.y - treeBitmap.height / 2f
                )
            )

            // draw particles
            for (particle in listOfNotNull(myParticle, otherParticle)) {
                when (particle.direction) {
                    1 -> drawWaterDroplet(
                        center = particle.center,
                        radius = TREE_PARTICLE_RADIUS,
                        alpha = particle.alpha
                    )

                    -1 -> drawSun(
                        center = particle.center,
                        radius = TREE_PARTICLE_RADIUS,
                        alpha = particle.alpha
                    )

                    else -> throw IllegalStateException("Invalid particle direction: ${particle.direction}")
                }
            }

            // draw leaves
            for ((topLeft, image, size) in leaves) {
                val scaledWidth = image.width * size.value
                val scaledHeight = image.height * size.value

                // Calculate the center of the image (based on original topLeft)
                val centerX = topLeft.x + image.width / 2f
                val centerY = topLeft.y + image.height / 2f

                // Compute new top-left so scaling happens around the center
                val newTopLeftX = centerX - scaledWidth / 2f
                val newTopLeftY = centerY - scaledHeight / 2f

                drawImage(
                    image = image,
                    dstSize = IntSize(scaledWidth.toInt(), scaledHeight.toInt()),
                    dstOffset = IntOffset(newTopLeftX.toInt(), newTopLeftY.toInt())
                )
            }
        }
    }
}

private fun DrawScope.drawWaterDroplet(
    center: Offset,
    radius: Float,
    alpha: Float = 1f
) {
    val controlOffsetX1 = 0.7f * radius   // controls upper narrowing
    val controlOffsetY1 = 0.8f * radius   // lower bulge
    val controlOffsetX2 = 0.95f * radius   // controls base width
    val controlOffsetY2 = 1.85f * radius   // vertical stretch toward tip

    val baseX = center.x
    val baseY = center.y - radius         // base at bottom of circle

    val path = Path().apply {
        moveTo(baseX, baseY) // bottom center
        // Left curve up to tip
        cubicTo(
            baseX - controlOffsetX1, baseY + controlOffsetY1,
            baseX - controlOffsetX2, baseY + controlOffsetY2,
            baseX, baseY + radius * 2f // top (tip)
        )
        // Right curve (mirror of left)
        cubicTo(
            baseX + controlOffsetX2, baseY + controlOffsetY2,
            baseX + controlOffsetX1, baseY + controlOffsetY1,
            baseX, baseY // back to bottom
        )
        close()
    }

    drawPath(path, Color(0xff000080), style = Fill, alpha = alpha)
}

private fun DrawScope.drawSun(
    center: Offset,
    radius: Float,
    alpha: Float = 1f
) {
    val innerRadius = radius * 0.45f
    val raysStartRadius = radius * 0.7f
    val numberOfRays = 8
    val rayStrokeWidth = radius * 0.2f
    val path = Path().apply {
        addArc(
            oval = Rect(
                left = center.x - innerRadius,
                top = center.y - innerRadius,
                right = center.x + innerRadius,
                bottom = center.y + innerRadius
            ),
            startAngleDegrees = 0f,
            sweepAngleDegrees = 360f
        )
        // draw rays
        // --- Draw the rays ---
        repeat(numberOfRays) { i ->
            val angleDeg = i * (360f / numberOfRays)
            val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()

            val start = Offset(
                x = center.x + raysStartRadius * cos(angleRad),
                y = center.y + raysStartRadius * sin(angleRad)
            )
            val end = Offset(
                x = center.x + radius * cos(angleRad),
                y = center.y + radius * sin(angleRad)
            )

            drawLine(
                color = Color(0xFFF8C934),
                start = start,
                end = end,
                strokeWidth = rayStrokeWidth,
                cap = StrokeCap.Round,
                alpha = alpha
            )
        }
    }

    drawPath(path, Color(0xFFF8C934), style = Fill, alpha = alpha)
}

private fun DrawScope.drawRing(
    center: Offset,
    radius: Float
) {
    // --- OUTER AQUA RING ---
    val ringWidth: Float = radius * 0.08f

    drawArc(
        brush = Brush.linearGradient(
            listOf(Color(0xFF7AD8D3), Color(0xFF53C3BD))
        ),
        startAngle = TREE_RING_START_ANGLE,
        sweepAngle = TREE_RING_SWEEP_ANGLE,
        useCenter = false,
        style = Stroke(width = ringWidth),
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2)
    )

    // --- INNER BLACK RIM ---
    val innerRadius = radius * 0.93f
    val innerRingWidth = innerRadius * 0.08f
    val tickLength = innerRadius * 0.1f
    val tickCount = 12

    // inner circular rim
    drawArc(
        color = Color.Black,
        startAngle = TREE_RING_START_ANGLE,
        sweepAngle = TREE_RING_SWEEP_ANGLE,
        useCenter = false,
        style = Stroke(width = innerRingWidth),
        topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
        size = Size(innerRadius * 2, innerRadius * 2)
    )

    // radial ticks
    repeat(tickCount) { i ->
        val angleDeg = 360f * i / tickCount
        if(angleDeg == 360f * i / (tickCount - 1)) return@repeat  // skip the top tick to avoid overlap
        val angleRad = Math.toRadians(angleDeg.toDouble())
        val inner = Offset(
            x = center.x + (innerRadius - tickLength * 1.3f) * cos(angleRad).toFloat(),
            y = center.y + (innerRadius - tickLength * 1.3f) * sin(angleRad).toFloat()
        )
        val outer = Offset(
            x = center.x + innerRadius * cos(angleRad).toFloat(),
            y = center.y + innerRadius * sin(angleRad).toFloat()
        )
        drawLine(
            color = Color.Black,
            start = inner,
            end = outer,
            strokeWidth = innerRingWidth * 0.7f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawTree(
    center: Offset,
    radius: Float
) {
    val foliageHeight = radius * 1.5f
    val foliageWidth = radius * 1.8f
    val trunkWidth = radius * 0.18f
    val trunkHeight = radius * 0.45f

    // --- brown trunk ---
    drawRect(
        color = Color(0xFF4A3324),
        topLeft = Offset(
            center.x - trunkWidth / 2f,
            center.y + foliageHeight / 2f - trunkHeight * 0.25f
        ),
        size = Size(trunkWidth, trunkHeight)
    )

    // --- main foliage shape ---
    val foliage = Path().apply {
        moveTo(center.x, center.y - foliageHeight / 2f) // top (before rounding)

        // left side curve down (narrow → wide)
        cubicTo(
            center.x - foliageWidth * 0.25f, center.y - foliageHeight * 0.2f,  // upper inward
            center.x - foliageWidth * 0.55f, center.y + foliageHeight * 0.35f, // widest
            center.x, center.y + foliageHeight / 2f                            // bottom center
        )

        // right side curve up (mirror)
        cubicTo(
            center.x + foliageWidth * 0.55f, center.y + foliageHeight * 0.35f,
            center.x + foliageWidth * 0.25f, center.y - foliageHeight * 0.2f,
            center.x, center.y - foliageHeight / 2f
        )

        close()
    }

    val capArc = Path().apply {
        val ovalCenter = Offset(center.x, center.y - radius * 0.34f)
        addOval(
            Rect(
                left = ovalCenter.x + radius*0.4f,
                right = ovalCenter.x - radius*0.4f,
                top = ovalCenter.y - radius * 0.4f,
                bottom = ovalCenter.y + radius * 0.5f
            )
        )
        val ovalCenter2 = Offset(center.x, center.y - radius * 0f)
        addOval(
            Rect(
                left = ovalCenter2.x + radius*0.485f,
                right = ovalCenter2.x - radius*0.485f,
                top = ovalCenter2.y - radius * 0.65f,
                bottom = ovalCenter2.y + radius * 0.65f
            )
        )
    }
    drawPath(path = capArc, color = Color(0xFF6FC84A))

    // --- boolean subtract to round the top ---
    val result = Path()
    result.op(foliage, capArc, PathOperation.Union)

    // --- draw the final rounded tree foliage ---
    drawPath(
        path = result,
        color = Color(0xFF6FC84A)
    )
}






