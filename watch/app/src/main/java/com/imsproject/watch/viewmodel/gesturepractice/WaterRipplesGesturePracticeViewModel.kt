package com.imsproject.watch.viewmodel.gesturepractice

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.imsproject.watch.BLUE_COLOR
import com.imsproject.watch.GRASS_GREEN_COLOR
import com.imsproject.watch.viewmodel.FlowerGardenViewModel.ItemType
import com.imsproject.watch.viewmodel.MainViewModel
import com.imsproject.watch.viewmodel.WaterRipplesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WaterRipplesGesturePracticeViewModel() : WaterRipplesViewModel() {

    private val _done = MutableStateFlow(false)
    val done: StateFlow<Boolean> = _done

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
        if(_done.value) return
        val ripple = Ripple(myColor,System.currentTimeMillis(),playerId)
        ripples.addFirst(ripple)
        _counter.value++

        if(_counter.value == 3){
            viewModelScope.launch {
                delay(1000)
                _done.value = true
            }
        }
    }

    fun reset(){
        _done.value = false
        _counter.value = 0
        ripples.clear()
    }

}