package com.imsproject.watch.view

import android.os.Bundle
import android.util.Log
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import com.imsproject.common.gameserver.GameType
import com.imsproject.watch.DARK_BACKGROUND_COLOR
import com.imsproject.watch.LIGHT_GRAY_COLOR
import com.imsproject.watch.SCREEN_CENTER
import com.imsproject.watch.SCREEN_RADIUS
import com.imsproject.watch.WATER_RIPPLES_ANIMATION_DURATION
import com.imsproject.watch.WATER_RIPPLES_BUTTON_SIZE
import com.imsproject.watch.viewmodel.GameViewModel
import com.imsproject.watch.viewmodel.WaterRipplesViewModel
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch

class WaterRipplesActivity : GameActivity(GameType.WATER_RIPPLES) {

    private val viewModel : WaterRipplesViewModel by viewModels<WaterRipplesViewModel>()

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
            GameViewModel.State.PLAYING -> WaterRipples(viewModel)
            else -> super.Main()
        }
        super.CheckConnection()
    }

    companion object {
        private const val TAG = "WaterRipplesActivity"
    }
}

@Composable
fun WaterRipples(viewModel: WaterRipplesViewModel) {
    val ripplesToShow = remember { mutableStateListOf<WaterRipplesViewModel.Ripple>() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        while(true)  {
            awaitFrame()
            viewModel.ripples.forEach { ripple ->
                if (ripple.animationStarted) return@forEach
                ripple.animationStarted = true
                ripplesToShow.add(ripple)
                val animation = Animatable(0f)
                scope.launch {
                    animation.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = WATER_RIPPLES_ANIMATION_DURATION,
                            easing = LinearEasing
                        )
                    ) {
                        ripple.size = (SCREEN_RADIUS - WATER_RIPPLES_BUTTON_SIZE) * value + WATER_RIPPLES_BUTTON_SIZE
                        ripple.currentAlpha = ripple.startingAlpha * (1f - value)
                    }
                    ripplesToShow.remove(ripple)
                    viewModel.ripples.remove(ripple)
                }
            }
        }
    }

    // Box to draw the background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = DARK_BACKGROUND_COLOR)
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
                    BorderStroke(2.dp, DARK_BACKGROUND_COLOR.copy(alpha = 0.5f)),
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
                backgroundColor = LIGHT_GRAY_COLOR,
                contentColor = Color.Black
            )
        ){
            // empty button content
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawContext.canvas.saveLayer(bounds = size.toRect(), paint = Paint())
            // Precompute all ripple paths
            val ripples = ripplesToShow.toTypedArray()
            val ripplePaths = ripples.map { ripple ->
                val outer = Path().apply {
                    addOval(
                        Rect(
                            center = SCREEN_CENTER,
                            radius = ripple.size + SCREEN_RADIUS * 0.035f
                        )
                    )
                }
                val inner = Path().apply {
                    addOval(
                        Rect(
                            center = SCREEN_CENTER,
                            radius = ripple.size - SCREEN_RADIUS * 0.035f
                        )
                    )
                }
                outer.apply {
                    op(this,inner, PathOperation.Difference)
                }
            }

            // --- Draw all base rings ---
            ripplePaths.forEachIndexed { index, path ->
                val ripple = ripples[index]
                drawPath(
                    path = path,
                    color = ripple.color.copy(alpha = ripple.currentAlpha)
                )
            }

            // --- Draw overlaps ---
            for (i in 0 until ripplePaths.size - 1) {
                if(ripples[i].actor == ripples[i+1].actor) continue // skip same-actor ripples
                val path1 = ripplePaths[i]
                val path2 = ripplePaths[i+1]
                val intersection = Path()

                if (intersection.op(path1, path2, PathOperation.Intersect)) {
                    // Mix the two colors toward white to emphasize brightness
                    val c1 = ripples[i].color
                    val c2 = ripples[i+1].color
                    val blend = c1.lerpTo(c2, 0.5f).lerpTo(Color.White, 0.5f)

                    drawPath(
                        path = intersection,
                        color = blend.copy(alpha = (ripples[i].currentAlpha + ripples[i+1].currentAlpha)),
                        blendMode = BlendMode.Plus
                    )
                }
            }
            drawContext.canvas.restore()
        }
    }
}

fun Color.lerpTo(target: Color, t: Float): Color {
    val clampedT = t.coerceIn(0f, 1f)
    return Color(
        red = red + (target.red - red) * clampedT,
        green = green + (target.green - green) * clampedT,
        blue = blue + (target.blue - blue) * clampedT,
        alpha = alpha + (target.alpha - alpha) * clampedT
    )
}

