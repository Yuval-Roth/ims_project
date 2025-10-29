package com.imsproject.watch.viewmodel.gesturepractice

import android.content.Context
import com.imsproject.watch.BLUE_COLOR
import com.imsproject.watch.GRASS_GREEN_COLOR
import com.imsproject.watch.viewmodel.FlowerGardenViewModel.ItemType
import com.imsproject.watch.viewmodel.MainViewModel
import com.imsproject.watch.viewmodel.WaterRipplesViewModel

class WaterRipplesGesturePracticeViewModel() : WaterRipplesViewModel() {

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
        val ripple = Ripple(myColor,System.currentTimeMillis(),playerId)
        ripples.addFirst(ripple)
        _counter.value++
    }

}