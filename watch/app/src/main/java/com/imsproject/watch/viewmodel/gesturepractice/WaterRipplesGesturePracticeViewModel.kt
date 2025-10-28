package com.imsproject.watch.viewmodel.gesturepractice

import com.imsproject.watch.BLUE_COLOR
import com.imsproject.watch.viewmodel.WaterRipplesViewModel

class WaterRipplesGesturePracticeViewModel : WaterRipplesViewModel() {

    override fun click() {
        val ripple = Ripple(BLUE_COLOR, timestamp = System.currentTimeMillis(), actor = playerId)
        ripples.addFirst(ripple)
        _counter.value++
    }

}