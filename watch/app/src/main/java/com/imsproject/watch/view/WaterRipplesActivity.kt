package com.imsproject.watch.view

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.view.WindowManager
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.lifecycle.viewModelScope
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import com.imsproject.common.gameserver.GameType
import com.imsproject.watch.DARK_BACKGROUND_COLOR
import com.imsproject.watch.LIGHT_GRAY_COLOR
import com.imsproject.watch.R
import com.imsproject.watch.RIPPLE_MAX_SIZE
import com.imsproject.watch.SCREEN_RADIUS
import com.imsproject.watch.WATER_RIPPLES_BUTTON_SIZE
import com.imsproject.watch.initProperties
import com.imsproject.watch.viewmodel.GameViewModel
import com.imsproject.watch.viewmodel.WaterRipplesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WaterRipplesActivity : GameActivity(GameType.WATER_RIPPLES) {

    private val viewModel : WaterRipplesViewModel by viewModels<WaterRipplesViewModel>()
    private lateinit var soundPool: SoundPool
    private var clickSoundId : Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.onCreate(viewModel)
        soundPool = SoundPool.Builder().setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build()).setMaxStreams(1).build()
        clickSoundId = soundPool.load(applicationContext, R.raw.ripple_click_sound, 1)
        setContent {
            Main()
        }
    }

    @Composable
    override fun Main(){
        val state by viewModel.state.collectAsState()
        when(state){
            GameViewModel.State.PLAYING -> WaterRipples()
            else -> super.Main()
        }
    }

    @SuppressLint("ReturnFromAwaitPointerEventScope")
    @Composable
    fun WaterRipples() {
        val ripples = remember { viewModel.ripples }

        // this is used only to to trigger recomposition when new ripples are added
        viewModel.counter.collectAsState().value

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
                                    viewModel.viewModelScope.launch(Dispatchers.Default) {
                                        soundPool.play(clickSoundId, 1f, 1f, 0, 0, 1f)
                                    }
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

            // Draw the ripples
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                for(ripple in ripples){
                    val color = ripple.color
                    val size = ripple.size
                    val alpha = ripple.currentAlpha

                    drawCircle(
                        color = color.copy(alpha = alpha),
                        radius = size,
                        style = Stroke(width = 4.dp.toPx())
                    )
                }
            }
        }

        // Ripple animation loop
        // We set the parameter to Unit because we continuously iterate over the ripples
        // and we don't need to cancel the LaunchedEffect ever
        LaunchedEffect(Unit){
            while(true){
                val rippleIterator = ripples.iterator()
                while (rippleIterator.hasNext()) {
                    val ripple = rippleIterator.next()

                    // remove ripples that are done animating
                    if(ripple.size >= RIPPLE_MAX_SIZE){
                        rippleIterator.remove()
                        continue
                    }

                    // animation step
                    ripple.size += ripple.sizeStep
                    ripple.currentAlpha = if(ripple.size >= RIPPLE_MAX_SIZE){
                        0f
                    } else {
                        // Sometimes the step can make the alpha drop below 0 so we coerce it to at least 0
                        (ripple.currentAlpha - ripple.alphaStep).fastCoerceAtLeast(0f)
                    }
                }
                delay(16)
            }
        }
    }

    companion object {
        private const val TAG = "WaterRipplesActivity"
    }
}

