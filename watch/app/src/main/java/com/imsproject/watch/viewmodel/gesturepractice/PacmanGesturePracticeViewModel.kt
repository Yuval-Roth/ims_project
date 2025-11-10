package com.imsproject.watch.viewmodel.gesturepractice

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewModelScope
import com.imsproject.watch.utils.Angle
import com.imsproject.watch.R
import com.imsproject.watch.view.Pacman
import com.imsproject.watch.viewmodel.MainViewModel
import com.imsproject.watch.viewmodel.PacmanViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PacmanGesturePracticeViewModel() : PacmanViewModel(), GesturePracticeViewModel {

    private val _done = MutableStateFlow(false)
    override val done: StateFlow<Boolean> = _done

    private var counter = 0
    private var timeout = 0L

    fun init(context: Context, playerColor: MainViewModel.PlayerColor) {
        soundPool = SoundPool.Builder().setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build()).setMaxStreams(1).build()
        rewardSoundId = soundPool.load(context, R.raw.pacman_eat2, 1)
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
        if (_myParticle.value == null) {
            return
        }
        handleFling(dpPerSec, myDirection, false)

        counter++
        if(counter == 3){
            viewModelScope.launch {
                delay(timeout)
                _done.value = true
            }
        }
    }

    override fun reset(){
        _done.value = false
        counter = 0
    }

    @Composable
    override fun RunGesturePractice(targetReachedTimeout: Long) {
        timeout = targetReachedTimeout
        Pacman(this)
    }
}