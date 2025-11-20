package com.imsproject.watch.viewmodel.gesturepractice

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewModelScope
import com.imsproject.watch.view.FlowerGarden
import com.imsproject.watch.viewmodel.FlowerGardenViewModel
import com.imsproject.watch.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FlowerGardenGesturePracticeViewModel() : FlowerGardenViewModel(), GesturePracticeViewModel {

    private val _done = MutableStateFlow(false)
    override val done: StateFlow<Boolean> = _done

    private var timeout = 0L
    private var counter = 0

    fun init(context: Context, playerColor: MainViewModel.PlayerColor) {
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
        waterDropletSets.clear()
        grassPlantSets.clear()
    }

    @Composable
    override fun RunGesturePractice(targetReachedTimeout: Long) {
        timeout = targetReachedTimeout
        FlowerGarden(this)
    }

}