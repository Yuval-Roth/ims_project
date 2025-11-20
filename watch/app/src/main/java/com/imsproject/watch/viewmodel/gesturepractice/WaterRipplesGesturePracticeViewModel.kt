package com.imsproject.watch.viewmodel.gesturepractice

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewModelScope
import com.imsproject.watch.BLUE_COLOR
import com.imsproject.watch.GRASS_GREEN_COLOR
import com.imsproject.watch.view.WaterRipples
import com.imsproject.watch.viewmodel.MainViewModel
import com.imsproject.watch.viewmodel.WaterRipplesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WaterRipplesGesturePracticeViewModel() : WaterRipplesViewModel(), GesturePracticeViewModel {

    private val _done = MutableStateFlow(false)
    override val done: StateFlow<Boolean> = _done

    private var timeout = 0L
    private var counter = 0

    fun init(context: Context, playerColor: MainViewModel.PlayerColor) {
        when(playerColor) {
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

    override fun click() {
        val ripple = Ripple(myColor,System.currentTimeMillis(),playerId, 0.35f)
        ripples.addFirst(ripple)
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
        ripples.clear()
    }

    @Composable
    override fun RunGesturePractice(targetReachedTimeout: Long) {
        timeout = targetReachedTimeout
        WaterRipples(this)
    }

}