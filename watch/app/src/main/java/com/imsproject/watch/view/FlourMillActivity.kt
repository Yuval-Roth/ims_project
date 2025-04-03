package com.imsproject.watch.view

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.imsproject.common.gameserver.GameType
import com.imsproject.watch.AXLE_WIDTH
import com.imsproject.watch.BROWN_COLOR
import com.imsproject.watch.DARK_BACKGROUND_COLOR
import com.imsproject.watch.GLOWING_YELLOW_COLOR
import com.imsproject.watch.LIGHT_BROWN_COLOR
import com.imsproject.watch.SCREEN_RADIUS
import com.imsproject.watch.initProperties
import com.imsproject.watch.utils.Angle
import com.imsproject.watch.utils.cartesianToPolar
import com.imsproject.watch.utils.polarToCartesian
import com.imsproject.watch.viewmodel.FlourMillViewModel
import com.imsproject.watch.viewmodel.GameViewModel
import androidx.compose.ui.input.pointer.PointerEventType
import com.imsproject.watch.BRIGHT_CYAN_COLOR

class FlourMillActivity : GameActivity(GameType.FLOUR_MILL) {

    private val viewModel : FlourMillViewModel by viewModels<FlourMillViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val metrics = getSystemService(WindowManager::class.java).currentWindowMetrics
        initProperties(metrics.bounds.width(), metrics.bounds.height())
        viewModel.onCreate(intent,applicationContext)
        setupUncaughtExceptionHandler(viewModel)
        setContent {
            Main()
        }
    }

    @Composable
    fun Main(){
        val state by viewModel.state.collectAsState()
        when(state){
            GameViewModel.State.PLAYING -> {
                FlourMill()
            }
            else -> super.Main(viewModel)}
    }

    @Composable
    fun FlourMill() {
        val (centerX, centerY) = remember { polarToCartesian(0f, 0.0) }
        val axle = remember { viewModel.axle }
        var touchPoint by remember { mutableStateOf(-1f to -1f) }
        var showAxle by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val pointerEvent = awaitPointerEvent()
                            val inputChange = pointerEvent.changes.first()
                            inputChange.consume()
                            val position = inputChange.position
                            when (pointerEvent.type) {
                                PointerEventType.Press -> {
                                    val (_, angle) = cartesianToPolar(position.x, position.y)
                                    axle.angle = angle + -90f
                                    touchPoint = position.x to position.y
                                    showAxle = true
                                }
                                PointerEventType.Move -> {
                                    viewModel.setTouchPoint(position.x, position.y)
                                    touchPoint = position.x to position.y
                                }
                                PointerEventType.Release -> {
                                    viewModel.setTouchPoint(-1f, -1f)
                                    touchPoint = -1f to -1f
                                    showAxle = false
                                }
                            }
                        }
                    }
                }
            ,
            contentAlignment = Alignment.Center

        ){
            Box(
                modifier = Modifier.Companion
                    .fillMaxSize()
                    .background(color = BROWN_COLOR)
            )
            Box( // ground
                modifier = Modifier.Companion
                    .fillMaxSize(0.8f)
                    .clip(shape = CircleShape)
                    .background(color = LIGHT_BROWN_COLOR)
                    .shadow(
                        elevation = (SCREEN_RADIUS * 0.5).dp,
                        CircleShape,
                        spotColor = Color.Green
                    )
                    .shadow(
                        elevation = (SCREEN_RADIUS * 0.5).dp,
                        CircleShape,
                        spotColor = Color.Green
                    )
            )

            Box( // center axis
                modifier = Modifier.Companion
                    .fillMaxSize(0.3f)
                    .clip(shape = CircleShape)
                    .background(color = GLOWING_YELLOW_COLOR)
            )

            Box( // center axis filler
                modifier = Modifier.Companion
                    .fillMaxSize(0.200f)
                    .clip(shape = CircleShape)
                    .background(color = DARK_BACKGROUND_COLOR)
            )
            Canvas(modifier = Modifier.Companion.fillMaxSize()){
                val touchCircleRadius = 30f
                val (leftX, leftY) = polarToCartesian(SCREEN_RADIUS * 0.7f, axle.angle + 90f)
                val (leftX2, leftY2) = polarToCartesian(SCREEN_RADIUS * 0.9f, axle.angle + 90f)
                val (rightX, rightY) = polarToCartesian(SCREEN_RADIUS * 0.9f, axle.angle + -90f)

                val (touchPointDistance,touchPointAngle) = cartesianToPolar(touchPoint.first, touchPoint.second)
                val touchPointInBounds = (touchPointAngle - (axle.angle + 90f) <= (360f / touchCircleRadius))
                        && touchPointDistance > (SCREEN_RADIUS * 0.7f - touchCircleRadius)
                        && touchPointDistance < (SCREEN_RADIUS * 0.9f + touchCircleRadius)

                val axlePath = Path().apply {
                    moveTo(rightX,rightY)
                    lineTo(leftX, leftY)
                }

                val axlePath2 = Path().apply {
                    moveTo(leftX,leftY)
                    lineTo(leftX2, leftY2)
                }

                if(showAxle){
                    drawPath(
                        path = axlePath,
                        color = GLOWING_YELLOW_COLOR,
                        style = Stroke(width = AXLE_WIDTH)
                    )
                    drawPath(
                        path = axlePath2,
                        color = BRIGHT_CYAN_COLOR,
                        style = Stroke(width = AXLE_WIDTH)
                    )
                    drawCircle(
                        radius = touchCircleRadius,
                        center = Offset(touchPoint.first, touchPoint.second),
                        color = if (touchPointInBounds) BRIGHT_CYAN_COLOR else BRIGHT_CYAN_COLOR.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }

//    @Composable
//    fun FlourMill() {
//        val (centerX, centerY) = remember { polarToCartesian(0f, 0f.toAngle()) }
//        val axle = remember { viewModel.axle }
//        val mySide = remember { viewModel.myAxleSide }
//        val focusRequester = remember { FocusRequester() }
//
//        Box(
//            modifier = Modifier.Companion
//                .fillMaxSize()
//                .background(color = DARK_BACKGROUND_COLOR)
//                .onRotaryScrollEvent {
//                    if (!viewModel.isCoolingDown()) {
//                        val direction = if (it.verticalScrollPixels < 0) -1 else 1
//                        viewModel.rotateMyAxleEnd(direction)
//                    }
//                    true
//                }
//                .focusRequester(focusRequester)
//                .focusable()
//                .pointerInput(Unit) {
//                    detectDragGestures(
//                        onDragEnd = {
//                            viewModel.onDragEnd()
//                        },
//                    ) { change, offset ->
//                        if (viewModel.isDragged()) return@detectDragGestures
//                        viewModel.dragged()
//
//                        if (!viewModel.isCoolingDown()) {
//                            val position = change.position
//                            val (_, angle) = cartesianToPolar(position.x, position.y)
//                            val direction = if (angle.isBetweenInclusive(-135.0001f, -45f)) {
//                                if (offset.x < 0) -1 else 1
//                            } else if (angle.isBetweenInclusive(-45.0001f, 45f)) {
//                                if (offset.y > 0) 1 else -1
//                            } else if (angle.isBetweenInclusive(45.0001f, 135f)) {
//                                if (offset.x < 0) 1 else -1
//                            } else {
//                                if (offset.y > 0) -1 else 1
//                            }
//                            viewModel.rotateMyAxleEnd(direction)
//                        }
//                    }
//                }
//            ,
//            contentAlignment = Alignment.Companion.Center
//        ) {
//
//            // Need to capture focus to enable rotary scroll event handling
//            LaunchedEffect(Unit) {
//                focusRequester.requestFocus()
//            }
//
//            // =============== Animate the axle and its ends ================ |
//
//            LaunchedEffect(Unit){
//                while(true){
//                    // left end
//                    animateAxleEndStep(axle, AxleSide.LEFT, mySide)
//
//                    // right end
//                    animateAxleEndStep(axle, AxleSide.RIGHT, mySide)
//
//                    // axle
//                    if(axle.angle != axle.targetAngle){
//                        val direction = getDirection(axle.angle, axle.targetAngle)
//                        axle.angle = axle.angle + STRETCH_STEP * direction
//                    } else {
//                        if(axle.isRotating){
//                            axle.effectiveAngle = axle.angle
//                            axle.isRotating = false
//                            viewModel.resetCoolDown()
//                        }
//                    }
//                    delay(16)
//                }
//            }
//
//            // =============== Draw background ================ |
//            Box(
//                modifier = Modifier.Companion
//                    .fillMaxSize()
//                    .background(color = BROWN_COLOR)
//            )
//            Box( // ground
//                modifier = Modifier.Companion
//                    .fillMaxSize(0.8f)
//                    .clip(shape = CircleShape)
//                    .background(color = LIGHT_BROWN_COLOR)
//                    .shadow(
//                        elevation = (SCREEN_RADIUS * 0.5).dp,
//                        CircleShape,
//                        spotColor = Color.Green
//                    )
//                    .shadow(
//                        elevation = (SCREEN_RADIUS * 0.5).dp,
//                        CircleShape,
//                        spotColor = Color.Green
//                    )
//            )
//
//            Box( // center axis
//                modifier = Modifier.Companion
//                    .fillMaxSize(0.3f)
//                    .clip(shape = CircleShape)
//                    .background(color = GLOWING_YELLOW_COLOR)
//            )
//
//            Box( // center axis filler
//                modifier = Modifier.Companion
//                    .fillMaxSize(0.200f)
//                    .clip(shape = CircleShape)
//                    .background(color = DARK_BACKGROUND_COLOR)
//            )
//
//            // =============== Draw axle ================ |
//
//            Canvas(modifier = Modifier.Companion.fillMaxSize()) {
//                for(side in AxleSide.entries){
//                    val axleEnd = axle.getEnd(side)
//                    val endAngle = axle.getEndAngle(side)
//
//                    val (bezierStartX,bezierStartY) = polarToCartesian(BEZIER_START_DISTANCE, endAngle)
//                    val (controlX,controlY) = polarToCartesian(CONTROL_POINT_DISTANCE, endAngle)
//                    val (stretchX,stretchY) = polarToCartesian(STRETCH_POINT_DISTANCE, axleEnd.angle)
//                    val (handleStartX,handleStartY) = calculateTriangleThirdPoint(
//                        centerX, centerY,
//                        stretchX, stretchY,
//                        AXLE_HANDLE_LENGTH / 2
//                    )
//                    val (handleEndX,handleEndY) = calculateTriangleThirdPoint(
//                        centerX, centerY,
//                        stretchX, stretchY,
//                        -AXLE_HANDLE_LENGTH / 2
//                    )
//
//                    val axlePath = Path().apply {
//                        moveTo(centerX, centerY)
//                        lineTo(bezierStartX, bezierStartY)
//                        quadraticTo(controlX, controlY, stretchX, stretchY)
//                    }
//
//                    val axleEndPath = Path().apply {
//                        moveTo(handleStartX,handleStartY)
//                        lineTo(handleEndX,handleEndY)
//                    }
//
//                    drawPath(
//                        path = axlePath,
//                        color = GLOWING_YELLOW_COLOR,
//                        style = Stroke(width = AXLE_WIDTH)
//                    )
//
//                    drawPath(
//                        path = axleEndPath,
//                        color = axleEnd.handleColor,
//                        style = Stroke(width = AXLE_WIDTH)
//                    )
//                }
//            }
//        }
//    }

//    private fun animateAxleEndStep(
//        axle: Axle,
//        animatedSide: AxleSide,
//        mySide : AxleSide
//    ) {
//        val axleEnd = axle.getEnd(animatedSide)
//
//        // First thing we do is get the angle of the axle end to its target angle
//        if (axleEnd.angle != axleEnd.targetAngle) {
//            val direction = getDirection(axleEnd.angle, axleEnd.targetAngle)
//            axleEnd.angle = axleEnd.angle + STRETCH_STEP * direction
//        }
//
//        // If the axle end is at its target angle, we check if there is more work to be done
//        else {
//
//            // if the stretch peak is not zero, then we're either resetting or waiting for sync timeout
//            if (axleEnd.stretchPeak != 0.0f) {
//
//                if (axleEnd.resetting) {
//                    // if the axle end is resetting, we set the target angle to be the next reset step
//                    val baseAngle = axle.getEffectiveEndAngle(animatedSide)
//                    val newStretchPeakSign = -1f * axleEnd.stretchPeak.sign()
//                    val newStretchPeak = (axleEnd.stretchPeak.absoluteValue - STRETCH_PEAK_DECAY) * newStretchPeakSign
//                    val newTargetAngle = baseAngle + newStretchPeak
//                    axleEnd.stretchPeak = newStretchPeak
//                    axleEnd.targetAngle = newTargetAngle
//
//                    // if the new stretch peak is zero, we finished resetting
//                    if (newStretchPeak == 0.0f) {
//                        axleEnd.resetting = false
//                        if(animatedSide == mySide){
//                            viewModel.resetCoolDown()
//                        }
//                    }
//                }
//
//                // we're waiting for the sync threshold timeout
//                else {
//                    // check if we passed the sync threshold timeout
//                    if (System.currentTimeMillis() >= axleEnd.syncThresholdTimeout) {
//                        if (viewModel.isSynced(animatedSide)) {
//                            axleEnd.stretchPeak = 0.0f // leave the axle end at the target angle
//                        } else {
//                            axleEnd.resetting = true // reset the axle end to its base angle
//                        }
//                    }
//                    //else ----> no work to be done
//                }
//            }
//            else {
//                if(!axle.isRotating && axleEnd.targetAngle != axle.getEffectiveEndAngle(animatedSide)){
//                    // if we get here then the axle end went out of sync with the axle for some reason
//                    // that i can't figure out. So we reset the axle end to its base angle according to the axle
//                    axleEnd.targetAngle = axle.getEffectiveEndAngle(animatedSide)
//                    if(animatedSide == mySide){
//                        viewModel.resetCoolDown()
//                    }
//                }
//                // else ----> no work to be done
//            }
//        }
//    }

    private fun getDirection(angle: Angle, targetAngle: Angle) : Float {
        return if (Angle.isClockwise(angle, targetAngle)) 1f else -1f
    }

//    @Composable
//    fun Gear() {
//        val (centerX, centerY) = remember { polarToCartesian(0f, 0f) }
//        var gearAngle by remember { mutableFloatStateOf(0f) }
//        var lastAngle = remember { UNDEFINED_ANGLE }
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .pointerInput(Unit) {
//                    awaitPointerEventScope {
//                        while (true) {
//                            val pointerEvent = awaitPointerEvent()
//                            when (pointerEvent.type) {
//                                PointerEventType.Move, PointerEventType.Press -> {
//                                    val inputChange = pointerEvent.changes.first()
//                                    inputChange.consume()
//                                    val position = inputChange.position
//                                    val (_, angle) = cartesianToPolar(
//                                        position.x.toDouble(),
//                                        position.y.toDouble()
//                                    )
//                                    if (lastAngle == UNDEFINED_ANGLE) {
//                                        lastAngle = angle
//                                        continue
//                                    }
//                                    val diff = calculateAngleDiff(angle, lastAngle)
//                                    gearAngle = (gearAngle + (diff / 8f) * getDirection(lastAngle,angle)) % 360f
//                                    lastAngle = angle
//                                }
//
//                                PointerEventType.Release -> {
//                                    lastAngle = UNDEFINED_ANGLE
//                                }
//                            }
//                        }
//                    }
//                }
//            ,
//            contentAlignment = Alignment.Center
//
//        ){
//            Box(
//                modifier = Modifier.Companion
//                    .fillMaxSize()
//                    .background(color = BROWN_COLOR)
//            )
//            Box( // ground
//                modifier = Modifier.Companion
//                    .fillMaxSize(0.8f)
//                    .clip(shape = CircleShape)
//                    .background(color = LIGHT_BROWN_COLOR)
//                    .shadow(
//                        elevation = (SCREEN_RADIUS * 0.5).dp,
//                        CircleShape,
//                        spotColor = Color.Green
//                    )
//                    .shadow(
//                        elevation = (SCREEN_RADIUS * 0.5).dp,
//                        CircleShape,
//                        spotColor = Color.Green
//                    )
//            )
//
//            Box( // center axis
//                modifier = Modifier.Companion
//                    .fillMaxSize(0.3f)
//                    .clip(shape = CircleShape)
//                    .background(color = GLOWING_YELLOW_COLOR)
//            )
//
//            Box( // center axis filler
//                modifier = Modifier.Companion
//                    .fillMaxSize(0.200f)
//                    .clip(shape = CircleShape)
//                    .background(color = DARK_BACKGROUND_COLOR)
//            )
//            Canvas(modifier = Modifier.Companion.fillMaxSize()){
//                val gearRadius = SCREEN_RADIUS*0.9f
//
//                // gear body
//                drawCircle(
//                    color = GLOWING_YELLOW_COLOR,
//                    radius = gearRadius,
//                    style = Stroke(width = 25f)
//                )
//
//                // Gear teeth (simplified)
//                val teethCount = 24
//                val angleStep = 360f / teethCount
//                for (i in 0 until teethCount) {
//                    val angle = i * angleStep + gearAngle
//                    val (startX, startY) = polarToCartesian(gearRadius, angle)
//                    val (endX, endY) = polarToCartesian(gearRadius + 25f, angle)
//
////                    val startX: Float
////                    val startY: Float
////                    val endX: Float
////                    val endY: Float
////                    if(true){
////                        polarToCartesian(gearRadius, angle).also {
////                            startX = it.first
////                            startY = it.second
////                        }
////                        polarToCartesian(gearRadius + 25f, angle).also {
////                            endX = it.first
////                            endY = it.second
////                        }
////                    } else {
////                        polarToCartesian(gearRadius - 25f, angle).also {
////                            startX = it.first
////                            startY = it.second
////                        }
////                        polarToCartesian(gearRadius
////                            , angle).also {
////                            endX = it.first
////                            endY = it.second
////                        }
////                    }
//
//                    drawLine(
//                        color = GLOWING_YELLOW_COLOR,
//                        start = Offset(startX, startY),
//                        end = Offset(endX, endY),
//                        strokeWidth = 20f
//                    )
//                }
//
//                val (bezierStartX,bezierStartY) = polarToCartesian(BEZIER_START_DISTANCE, 0f)
//                val (controlX,controlY) = polarToCartesian(CONTROL_POINT_DISTANCE, 0f)
//                val (stretchX,stretchY) = polarToCartesian(STRETCH_POINT_DISTANCE, gearAngle)
//
//                val myAxleEndPath = Path().apply {
//                    moveTo(centerX, centerY)
//                    lineTo(bezierStartX, bezierStartY)
//                    quadraticTo(controlX, controlY, stretchX, stretchY)
//                }
//
//                drawPath(
//                    path = myAxleEndPath,
//                    color = GLOWING_YELLOW_COLOR,
//                    style = Stroke(width = AXLE_WIDTH)
//                )
//
//                // TODO: replace with actual data
//                val (bezierStartX2,bezierStartY2) = polarToCartesian(BEZIER_START_DISTANCE, -179.9999f)
//                val (controlX2,controlY2) = polarToCartesian(CONTROL_POINT_DISTANCE*0.8f, -179.9999f)
//                val (stretchX2,stretchY2) = polarToCartesian(STRETCH_POINT_DISTANCE*0.8f, -179.9999f)
//                val (handleStartX,handleStartY) = calculateTriangleThirdPoint(
//                    centerX, centerY,
//                    stretchX2, stretchY2,
//                    AXLE_HANDLE_LENGTH / 2
//                )
//                val (handleEndX,handleEndY) = calculateTriangleThirdPoint(
//                    centerX, centerY,
//                    stretchX2, stretchY2,
//                    -AXLE_HANDLE_LENGTH / 2
//                )
//
//                val otherAxleEndPath = Path().apply {
//                    moveTo(centerX, centerY)
//                    lineTo(bezierStartX2, bezierStartY2)
//                    quadraticTo(controlX2, controlY2, stretchX2, stretchY2)
//                }
//
//                drawPath(
//                    path = otherAxleEndPath,
//                    color = BRIGHT_CYAN_COLOR,
//                    style = Stroke(width = AXLE_WIDTH)
//                )
//
//                drawLine(
//                    color = BRIGHT_CYAN_COLOR,
//                    start = Offset(handleStartX,handleStartY),
//                    end = Offset(handleEndX,handleEndY),
//                    strokeWidth = AXLE_WIDTH
//                )
//
////                drawCircle(
////                    color = Color.Black,
////                    radius = gearRadius,
////                    style = Stroke(width = 3f)
////                )
//            }
//        }
//    }

    companion object {
        private const val TAG = "FlourMillActivity"
    }
}