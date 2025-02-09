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
import androidx.compose.runtime.*
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
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val metrics = getSystemService(WindowManager::class.java).currentWindowMetrics
        initProperties(metrics.bounds.width(), metrics.bounds.height())
        viewModel.onCreate(intent,applicationContext)
        soundPool = SoundPool.Builder().setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build()).setMaxStreams(1).build()
        clickSoundId = soundPool.load(applicationContext, R.raw.ripple_click_sound, 1)
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
                WaterRipples()
            }
            else -> super.Main(viewModel)
        }
    }

    @SuppressLint("ReturnFromAwaitPointerEventScope")
    @Composable
    fun WaterRipples() {
        val ripples = remember { viewModel.ripples }

        // this is used only to to trigger recomposition
        viewModel.counter.collectAsState().value

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

            Button(
                modifier = Modifier
                    .border(
                        BorderStroke(2.dp, DARK_BACKGROUND_COLOR.copy(alpha = 0.5f)),
                        CircleShape
                    )
                    .size(WATER_RIPPLES_BUTTON_SIZE.dp)
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
                onClick = {},
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = LIGHT_GRAY_COLOR,
                    contentColor = Color.Black
                )
            ){
                // button content
            }

            // ================================================= |
            // =============== Ripple Effect =================== |
            // ================================================= |

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
                            (ripple.currentAlpha - ripple.alphaStep).fastCoerceAtLeast(0f)
                        }
                    }
                    delay(16)
                }
            }

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
    }

    companion object {
        private const val TAG = "WaterRipplesActivity"
    }
}

