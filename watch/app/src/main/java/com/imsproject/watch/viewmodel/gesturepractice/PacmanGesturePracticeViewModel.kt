package com.imsproject.watch.viewmodel.gesturepractice

import android.content.Context
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
import com.imsproject.watch.utils.FrequencyTracker
import com.imsproject.watch.utils.WavPlayer
import com.imsproject.watch.utils.cartesianToPolar
import com.imsproject.watch.viewmodel.FlowerGardenViewModel.ItemType
import com.imsproject.watch.viewmodel.MainViewModel
import com.imsproject.watch.viewmodel.PacmanViewModel
import com.imsproject.watch.viewmodel.WaterRipplesViewModel
import com.imsproject.watch.viewmodel.WineGlassesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PacmanGesturePracticeViewModel() : PacmanViewModel() {

    private val _done = MutableStateFlow(false)
    val done: StateFlow<Boolean> = _done

    private var counter = 0

    fun init(context: Context, playerColor: MainViewModel.PlayerColor) {
        soundPool = SoundPool.Builder().setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build()).setMaxStreams(1).build()
        flingSoundId = soundPool.load(context, R.raw.pacman_eat2, 1)
        _animatePacman.value = false
        myDirection = when(playerColor) {
            MainViewModel.PlayerColor.BLUE -> {
                _showRightSide.value = false
                pacmanAngle.value = Angle(180f)
                1
            }
            MainViewModel.PlayerColor.GREEN -> {
                _showLeftSide.value = false
                pacmanAngle.value = Angle(0f)
                -1
            }
        }
        myStartTime = System.currentTimeMillis()
        viewModelScope.launch(Dispatchers.Main) {
            delay(1000L)
            _myParticle.value = createNewParticle(myDirection)
        }
    }

    override fun fling(dpPerSec: Float) {
        if(_done.value) return
        if (_myParticle.value == null) {
            return
        }
        handleFling(dpPerSec, myDirection, false)

        counter++
        if(counter == 3){
            viewModelScope.launch {
                delay(1000)
                _done.value = true
            }
        }
    }

    fun reset(){
        _done.value = false
        counter = 0
    }
}