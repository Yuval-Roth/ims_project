package com.imsproject.watch.view

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentSanitizer
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import com.imsproject.watch.DARK_BACKGROUND_COLOR
import com.imsproject.watch.PACKAGE_PREFIX
import com.imsproject.watch.SCREEN_WIDTH
import com.imsproject.watch.initGlobalValues
import com.imsproject.watch.textStyle
import com.imsproject.watch.view.contracts.Result
import com.imsproject.watch.viewmodel.GameViewModel
import com.imsproject.watch.viewmodel.WineGlassesViewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.wear.compose.material.SwipeToDismissBox
import androidx.wear.compose.material.rememberSwipeToDismissBoxState
import com.imsproject.watch.LIGHT_BLUE_COLOR
import com.imsproject.watch.WATER_RIPPLES_ANIMATION_DURATION
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt
import kotlinx.coroutines.delay

val GLOW_COLOR = Color(0xFFFFA500) // Example orange glow color
private const val MARKER_FADE_DURATION = 500
private const val MARKER_SIZE = 30

class WineGlassesActivity : ComponentActivity() {

//    private val viewModel : WineGlassesViewModel by viewModels<WineGlassesViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val metrics = getSystemService(WindowManager::class.java).currentWindowMetrics
        initGlobalValues(metrics.bounds.width(), metrics.bounds.height())
//        viewModel.onCreate(intent)
        setContent {
            WineGlasses()
//            Row(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .background(color = DARK_BACKGROUND_COLOR)
//                    .horizontalScroll(rememberScrollState()),
//                horizontalArrangement = Arrangement.Center
//            ) {
////                Main()
//
//            }
        }
    }

//    @Composable
//    fun Main(){
//        val state by viewModel.state.collectAsState()
//        when(state){
//            GameViewModel.State.LOADING -> {
//                LoadingScreen()
//            }
//            GameViewModel.State.PLAYING -> {
//                WineGlasses()
//            }
//            GameViewModel.State.TERMINATED -> {
//                val result = viewModel.resultCode.collectAsState().value
//                val intent = IntentSanitizer.Builder()
//                    .allowComponent(componentName)
//                    .build().sanitize(intent) {
//                        Log.d(TAG, "WineGlasses: $it")
//                    }
//                if(result != Result.Code.OK){
//                    intent.putExtra("$PACKAGE_PREFIX.error", viewModel.error.collectAsState().value)
//                }
//                setResult(result.ordinal,intent)
//                finish()
//            }
//        }
//    }

    @Composable
    private fun LoadingScreen() {
        MaterialTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DARK_BACKGROUND_COLOR),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        strokeWidth = (SCREEN_WIDTH.toFloat() * 0.02f).dp,
                        modifier = Modifier.size((SCREEN_WIDTH *0.2f).dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    BasicText(
                        text = "Loading session...",
                        style = textStyle
                    )
                }
            }
        }
    }

    @Composable
    fun WineGlasses() {
        var touchPoint = remember { mutableStateOf<Pair<Double,Double>>(Pair(-1.0,-1.0)) }
        val center = remember {Offset(SCREEN_WIDTH / 2f, SCREEN_WIDTH / 2f)}
        val radiusOuterEdge = remember{ (SCREEN_WIDTH / 2) }
        val radiusInnerEdge = remember { (SCREEN_WIDTH / 2) * 0.2f }
        val radiusCenter = remember { (SCREEN_WIDTH / 2) * 0.7f }
        val defaultAlpha = remember { 0.8f }
        val alpha = remember { mutableFloatStateOf(0.8f) }
        val counter = remember { mutableIntStateOf(0) }
        val released = remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = DARK_BACKGROUND_COLOR)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Release) {
                                released.value = true
                                counter.intValue++
                            } else {
                                released.value = false
                                alpha.floatValue = defaultAlpha
                                val eventFirst = event.changes.first()
                                eventFirst.consume()
                                val touchPosition = eventFirst.position
                                touchPoint.value =
                                    Pair(touchPosition.x.toDouble(), touchPosition.y.toDouble())
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            LaunchedEffect(counter.intValue) {
                if(released.value){
                    val alphaAnimStep =  defaultAlpha / (MARKER_FADE_DURATION / 16f)
                    while(released.value && alpha.floatValue > 0.0f){
                        alpha.floatValue = (alpha.floatValue - alphaAnimStep).coerceAtLeast(0.0f)
                        delay(16)
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize(0.8f)
                    .clip(shape = CircleShape)
                    .background(color = LIGHT_BLUE_COLOR)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize(0.6f)
                    .clip(shape = CircleShape)
                    .background(color = DARK_BACKGROUND_COLOR)
            )
            Canvas(modifier = Modifier.fillMaxSize()) {

                // calculate distance from center
                // to determine if the touch point is roughly within the circle
                // we allow a little bit of leeway
                val distanceFromCenter = sqrt(
                    (touchPoint.value.first - center.x).pow(2.0) +
                            (touchPoint.value.second - center.y).pow(2.0))

                //calculate angle from coordinates
                val angle = Math.toRadians(Math.toDegrees(
                    atan2(
                        touchPoint.value.second - center.y,
                        touchPoint.value.first - center.x
                    )
                ))

                //calculate x and y to draw the circle
                val x = center.x + radiusCenter * kotlin.math.cos(angle).toFloat()
                val y = center.y + radiusCenter * kotlin.math.sin(angle).toFloat()

                if(distanceFromCenter >= radiusInnerEdge && distanceFromCenter <= radiusOuterEdge){
                    if(touchPoint.value.first != -1.0 && touchPoint.value.second != -1.0){
                        drawCircle(
                            color = GLOW_COLOR.copy(alpha = alpha.floatValue),
                            center = Offset(x, y),
                            radius = MARKER_SIZE.dp.toPx(),
                            style = Fill
                        )
                    }
                }
            }
        }
    }


    companion object {
        private const val TAG = "WineGlassesActivity"
    }

    @Preview(device = "id:wearos_large_round", apiLevel = 34)
    @Composable
    fun PreviewLoadingScreen() {
        initGlobalValues(454, 454)
        LoadingScreen()
    }

    @Preview(device = "id:wearos_large_round", apiLevel = 34)
    @Composable
    fun PreviewWineGlasses() {
        initGlobalValues(454, 454)
        WineGlasses()
    }
}

