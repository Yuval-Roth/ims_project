package com.imsproject.watch.viewmodel.gesturepractice

import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewModelScope
import com.imsproject.watch.R
import com.imsproject.watch.view.Waves
import com.imsproject.watch.viewmodel.MainViewModel
import com.imsproject.watch.viewmodel.WavesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WavesGesturePracticeViewModel() :  WavesViewModel(), GesturePracticeViewModel {

    private val _done = MutableStateFlow(false)
    override val done: StateFlow<Boolean> = _done

    private var counter = 0
    private var timeout = 0L

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
                delay(timeout)
                _done.value = true
            }
        }
    }

    override fun reset(){
        counter = 0
        _done.value = false
    }

    @Composable
    override fun RunGesturePractice(targetReachedTimeout: Long) {
        timeout = targetReachedTimeout
        Waves(this)
    }
}