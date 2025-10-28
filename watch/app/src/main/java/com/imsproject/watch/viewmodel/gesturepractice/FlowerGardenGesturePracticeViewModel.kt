package com.imsproject.watch.viewmodel.gesturepractice

import com.imsproject.watch.BLUE_COLOR
import com.imsproject.watch.GRASS_GREEN_COLOR
import com.imsproject.watch.viewmodel.FlowerGardenViewModel
import com.imsproject.watch.viewmodel.MainViewModel
import com.imsproject.watch.viewmodel.WaterRipplesViewModel

class FlowerGardenGesturePracticeViewModel() : FlowerGardenViewModel() {

    var playerColor: MainViewModel.PlayerColor = MainViewModel.PlayerColor.BLUE
        set(value) {
            field = value
            myItemType = when(playerColor) {
                MainViewModel.PlayerColor.BLUE -> ItemType.WATER
                MainViewModel.PlayerColor.GREEN -> ItemType.PLANT
            }
        }


    override fun click() {
        if(myItemType == ItemType.WATER) {
            waterDropletSets.addLast(WaterDroplet(System.currentTimeMillis(), myItemType))
        } else {
            grassPlantSets.addLast(Plant(System.currentTimeMillis(), myItemType))
        }
        _counter.value++
    }

}