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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.lifecycle.viewModelScope
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import com.imsproject.common.gameserver.GameType
import com.imsproject.common.utils.Angle
import com.imsproject.watch.DARK_BACKGROUND_COLOR
import com.imsproject.watch.LIGHT_GRAY_COLOR
import com.imsproject.watch.R
import com.imsproject.watch.RIPPLE_MAX_SIZE
import com.imsproject.watch.SCREEN_RADIUS
import com.imsproject.watch.WATER_RIPPLES_BUTTON_SIZE
import com.imsproject.watch.initProperties
import com.imsproject.watch.utils.polarToCartesian
import com.imsproject.watch.viewmodel.FlowerGardenViewModel
import com.imsproject.watch.viewmodel.GameViewModel
import com.imsproject.watch.viewmodel.WaterRipplesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

class FlowerGardenActivity : GameActivity(GameType.FLOWER_GARDEN) {

    private val viewModel : FlowerGardenViewModel by viewModels<FlowerGardenViewModel>()
//    private lateinit var soundPool: SoundPool
    private var clickSoundId : Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.onCreate(viewModel)
//        soundPool = SoundPool.Builder().setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build()).setMaxStreams(1).build()
//        clickSoundId = soundPool.load(applicationContext, R.raw.ripple_click_sound, 1)
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

            Canvas(
                modifier = Modifier.size(100.dp)
            ) {
                val w = size.width
                val h = size.height
                val centerX = w/2f
                val centerY = h/2f

                if (viewModel.waterDroplet.visible) {
                    drawCircle(
                        viewModel.waterDroplet.color,
                        radius = h / 5f,
                        style = Fill,
                        center = Offset(x = centerX, y = centerY - 4*h/7)
                    )
                }

                if (viewModel.plant.visible) {
                    drawCircle(
                        viewModel.plant.color,
                        radius = h / 5f,
                        style = Fill,
                        center = Offset(x = centerX , y = centerY + 4*h/7)
                    )
                }

                if (viewModel.flower.visible) {
                    drawCircle(
                        viewModel.flower.color,
                        radius = h / 5f,
                        style = Fill,
                        center = Offset(x = centerX - 4*w/7, y = centerY)
                    )
                }
            }
        }

        // Ripple animation loop
        // We set the parameter to Unit because we continuously iterate over the ripples
        // and we don't need to cancel the LaunchedEffect ever
        LaunchedEffect(Unit){
            while(true) {
                if(viewModel.waterDroplet.visible) {
                    val nextAlpha = max(viewModel.waterDroplet.color.alpha - 0.01f, 0f)
                    viewModel.waterDroplet.color = viewModel.waterDroplet.color.copy(nextAlpha)
                    if(nextAlpha <= 0)
                        viewModel.waterDroplet.visible = false
                }

                if(viewModel.plant.visible) {
                    val nextAlpha = max(viewModel.plant.color.alpha - 0.01f, 0f)
                    viewModel.plant.color = viewModel.plant.color.copy(nextAlpha)
                    if(nextAlpha <= 0)
                        viewModel.plant.visible = false
                }

                if(viewModel.flower.visible) {
                    val nextAlpha = max(viewModel.flower.color.alpha - 0.01f, 0f)
                    viewModel.flower.color = viewModel.flower.color.copy(nextAlpha)
                    if(nextAlpha <= 0)
                        viewModel.flower.visible = false
                }

                delay(16)
            }
        }
    }

    companion object {
        private const val TAG = "WaterRipplesActivity"
    }
}

