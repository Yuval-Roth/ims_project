package com.imsproject.watch.viewmodel.gesturepractice

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.imsproject.common.utils.Angle
import com.imsproject.watch.BLUE_COLOR
import com.imsproject.watch.GRASS_GREEN_COLOR
import com.imsproject.watch.INNER_TOUCH_POINT
import com.imsproject.watch.OUTER_TOUCH_POINT
import com.imsproject.watch.utils.FrequencyTracker
import com.imsproject.watch.utils.WavPlayer
import com.imsproject.watch.utils.cartesianToPolar
import com.imsproject.watch.viewmodel.FlourMillViewModel
import com.imsproject.watch.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FlourMillGesturePracticeViewModel() : FlourMillViewModel() {

    private val _done = MutableStateFlow(false)
    val done: StateFlow<Boolean> = _done

    private var lastAngle = Angle.undefined
    private var accumulator: Float = 0f
    private var doneTriggered = false

    fun init(context: Context, playerColor: MainViewModel.PlayerColor) {
        wavPlayer = WavPlayer(context, viewModelScope)
        setupWavPlayer()
        myFrequencyTracker = FrequencyTracker()

        when (playerColor) {
            MainViewModel.PlayerColor.BLUE -> {
                myColor = BLUE_COLOR
                opponentColor = GRASS_GREEN_COLOR
            }

            MainViewModel.PlayerColor.GREEN -> {
                myColor = GRASS_GREEN_COLOR
                opponentColor = BLUE_COLOR
            }
        }
    }

    override fun setTouchPoint(x: Float, y: Float) {
        if(_done.value) return
        val (distance,rawAngle) = cartesianToPolar(x, y)
        val inBounds = if(x != -1.0f && y != -1.0f){
            distance in INNER_TOUCH_POINT..OUTER_TOUCH_POINT
        } else {
            false // not touching the screen
        }

        if(inBounds){
            myArc.updateArc(rawAngle)
            _released.value = false
            if(lastAngle != Angle.undefined){
                val angleDiff = rawAngle - lastAngle
                if(accumulator + angleDiff >= 360f){
                    if(doneTriggered) return
                    doneTriggered = true
                    viewModelScope.launch {
                        delay(1000)
                        _done.value = true
                    }
                } else {
                    accumulator += angleDiff
                }
            }
            lastAngle = rawAngle
        } else {
            _released.value = true
            lastAngle = Angle.undefined
        }
    }
}