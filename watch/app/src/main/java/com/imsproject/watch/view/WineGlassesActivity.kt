package com.imsproject.watch.view

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId

val GLOW_COLOR = Color(0xFFFFA500) // Example orange glow color

class WineGlassesActivity : ComponentActivity() {

//    private val viewModel : WineGlassesViewModel by viewModels<WineGlassesViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
        var touchPoint = remember { mutableStateOf<Pair<Float,Float>>(Pair(-1f,-1f)) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = DARK_BACKGROUND_COLOR)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while(true){
                            val event = awaitPointerEvent()
                            if(event.type == PointerEventType.Release){
                                touchPoint.value = Pair(-1f, -1f)
                                continue
                            }
                            val touchPosition = event.changes.first().position
                            touchPoint.value = Pair(touchPosition.x, touchPosition.y)
                        }
                    }
//                    detectDragGestures(
//                        onDragStart = { offset -> touchPoint.value = Pair(offset.x, offset.y) },
//                        onDrag = { change, dragAmount ->
//                            touchPoint.value = Pair(change.position.x, change.position.y)
//                        },
//                        onDragEnd = { touchPoint.value = Pair(-1f, -1f) }
//                    )
                },
//                .pointerInput(Unit) {
//                    detectTapGestures(
//                        onPress = { offset -> touchPoint.value = Pair(offset.x, offset.y) },
//                    )
//                }
//                .pointerInput()


            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
//                // Draw outer ring
//                drawCircle(
//                    color = DARK_BACKGROUND_COLOR.copy(alpha = 0.3f),
//                    radius = size.minDimension / 2 - 20.dp.toPx(),
//                    style = Stroke(width = 4.dp.toPx())
//                )

                println("touchPoint: $touchPoint")

                // Draw glowing effect for each touch point
                if(touchPoint.value.first != -1f && touchPoint.value.second != -1f){
                    drawCircle(
                        color = GLOW_COLOR.copy(alpha = 0.6f),
                        center = Offset(touchPoint.value.first, touchPoint.value.second),
                        radius = 5.dp.toPx(),
                        style = Fill
                    )
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

