package com.imsproject.watch.viewmodel.gesturepractice

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.imsproject.watch.BLUE_COLOR
import com.imsproject.watch.GRASS_GREEN_COLOR
import com.imsproject.watch.INNER_TOUCH_POINT
import com.imsproject.watch.OUTER_TOUCH_POINT
import com.imsproject.watch.utils.FrequencyTracker
import com.imsproject.watch.utils.WavPlayer
import com.imsproject.watch.utils.cartesianToPolar
import com.imsproject.watch.viewmodel.FlourMillViewModel
import com.imsproject.watch.viewmodel.MainViewModel

class FlourMillGesturePracticeViewModel() : FlourMillViewModel() {

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
        val (distance,rawAngle) = cartesianToPolar(x, y)
        val inBounds = if(x != -1.0f && y != -1.0f){
            distance in INNER_TOUCH_POINT..OUTER_TOUCH_POINT
        } else {
            false // not touching the screen
        }

        if(inBounds){
            myArc.updateArc(rawAngle)
            _released.value = false
        } else {
            _released.value = true
        }
    }
}