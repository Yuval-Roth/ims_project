package com.imsproject.watch.viewmodel.gesturepractice

import android.media.AudioAttributes
import android.media.SoundPool
import androidx.lifecycle.viewModelScope
import com.imsproject.common.utils.Angle
import com.imsproject.watch.ACTIVITY_DEBUG_MODE
import com.imsproject.watch.BLUE_COLOR
import com.imsproject.watch.GRASS_GREEN_COLOR
import com.imsproject.watch.INNER_TOUCH_POINT
import com.imsproject.watch.OUTER_TOUCH_POINT
import com.imsproject.watch.PACMAN_MOUTH_OPENING_ANGLE
import com.imsproject.watch.PACMAN_ROTATION_DURATION
import com.imsproject.watch.R
import com.imsproject.watch.utils.cartesianToPolar
import com.imsproject.watch.viewmodel.FlowerGardenViewModel.ItemType
import com.imsproject.watch.viewmodel.MainViewModel
import com.imsproject.watch.viewmodel.PacmanViewModel
import com.imsproject.watch.viewmodel.WaterRipplesViewModel
import com.imsproject.watch.viewmodel.WavesViewModel
import com.imsproject.watch.viewmodel.WineGlassesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WavesGesturePracticeViewModel() :  WavesViewModel() {

    private val _done = MutableStateFlow(false)
    val done: StateFlow<Boolean> = _done

    private var counter = 0

    fun init(context: android.content.Context, playerColor: MainViewModel.PlayerColor) {
        soundPool = SoundPool.Builder().setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build()).setMaxStreams(1).build()
        strongWaveSoundId = soundPool.load(context, R.raw.wave_strong, 1)
        mediumWaveSoundId = soundPool.load(context,R.raw.wave_medium,1)
        weakWaveSoundId = soundPool.load(context,R.raw.wave_weak,1)
        myDirection = when(playerColor) {
            MainViewModel.PlayerColor.BLUE -> 1
            MainViewModel.PlayerColor.GREEN -> -1
        }
        wave.value = Wave(myDirection)
        viewModelScope.launch {
            _turn.collect {
                if(it == -myDirection){
                    wave.value = Wave(myDirection)
                    _turn.value = myDirection
                }
            }
        }
    }

    override fun fling(dpPerSec: Float) {
        handleFling(dpPerSec, myDirection)

        counter++
        if(counter == 3){
            viewModelScope.launch {
                delay(1000)
                _done.value = true
            }
        }
    }
}