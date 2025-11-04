package com.imsproject.watch.view

import android.os.Bundle
import android.os.VibrationEffect
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.core.content.IntentSanitizer
import com.imsproject.common.gameserver.GameType
import com.imsproject.watch.DARK_BACKGROUND_COLOR
import com.imsproject.watch.PACKAGE_PREFIX
import com.imsproject.watch.TEXT_SIZE
import com.imsproject.watch.textStyle
import com.imsproject.watch.viewmodel.GameViewModel
import com.imsproject.watch.viewmodel.RecessViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.min

class RecessActivity: GameActivity(GameType.RECESS) {

    val viewModel: RecessViewModel by viewModels<RecessViewModel>()

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
            GameViewModel.State.PLAYING -> Recess(viewModel)
            GameViewModel.State.TERMINATED -> {
                BlankScreen()
                val result = viewModel.resultCode.collectAsState().value
                val intent = IntentSanitizer.Builder()
                    .allowComponent(componentName)
                    .build().sanitize(intent) {
                        Log.d(TAG, it)
                    }
                intent.putExtra("$PACKAGE_PREFIX.error", viewModel.error.collectAsState().value)
                intent.putExtra("$PACKAGE_PREFIX.uploadEvents", false)
                setResult(result.ordinal,intent)
                finish()
            }
            else -> super.Main()
        }
    }

    companion object {
        private const val TAG = "RecessActivity"
    }
}

@Composable
fun Recess(viewModel: RecessViewModel) {
    Column(
        modifier = Modifier
            .background(color = DARK_BACKGROUND_COLOR)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val currentNumber = remember { MutableStateFlow( viewModel.recessLength.value) }
        var alpha by remember { mutableFloatStateOf(1f) }
        val countdown = remember { Animatable(-1f) }
        val scope = rememberCoroutineScope()
        val clickVibration = remember {
            VibrationEffect.createOneShot(
                100, // duration in milliseconds
                255  // amplitude (0–255); 255 = strongest
            )
        }
        LaunchedEffect(Unit){
            // calculate precise duration
            val currentGameTime = viewModel.getCurrentGameTime()
            val recessLength = viewModel.recessLength.value
            val totalTimeLeft = recessLength * 1000 - currentGameTime
            val endTime = System.currentTimeMillis() + totalTimeLeft
            countdown.snapTo(totalTimeLeft / 1000f)

            scope.launch {
                currentNumber.collect {
                    // vibrate on each number change
                    if(it <= 5){
                        viewModel.vibrator.vibrate(clickVibration)
                    }
                }
            }
            countdown.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = (endTime - System.currentTimeMillis()).toInt(),
                    easing = LinearEasing
                )
            ) {
                if(value == 0f){
                    currentNumber.value = 0
                    alpha = 1f
                } else if(value <= 5){
                    currentNumber.value = ceil(value).toInt()
                    alpha = (1.111f*(value - value.toInt()) - 0.111f).fastCoerceAtLeast(0f)
                } else {
                    // ceil to nearest multiple of 10 or recessLength if exceeded
                    currentNumber.value = recessLength
                    alpha = if(value <= 6f){
                        (1.111f*(value - 5f) - 0.111f).fastCoerceAtLeast(0f)
                    } else {
                        1f
                    }
                }
            }
        }
        Spacer(modifier = Modifier.fillMaxHeight(0.1f))
        RTLText(
            text = "הפסקה קצרה",
            style = textStyle.copy(fontSize = TEXT_SIZE * 1.25f, textDecoration = TextDecoration.Underline),
        )
        Spacer(modifier = Modifier.fillMaxHeight(0.1f))
        RTLText(
            text = "חוזרים בעוד",
            style = textStyle.copy(fontSize = TEXT_SIZE),
        )
        Spacer(modifier = Modifier.fillMaxHeight(0.05f))
        val number = currentNumber.collectAsState().value
        if(number >= 0){
            Text(
                modifier = Modifier.alpha(alpha),
                text = number.toString(),
                style = textStyle.copy(fontSize = TEXT_SIZE * 4f)
            )
        }
        Spacer(modifier = Modifier.fillMaxHeight(0.05f))
        RTLText(
            text = "שניות",
            style = textStyle.copy(fontSize = TEXT_SIZE),
        )
    }
}