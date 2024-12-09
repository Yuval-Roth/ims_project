package com.imsproject.watch.viewmodel

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

private const val DARK_BACKGROUND_COLOR = 0xFF333842
private const val VIVID_ORANGE_COLOR = 0xFFFF5722
private const val LIGHT_BLUE_COLOR = 0xFFACC7F6

private const val BUTTON_SIZE = 80

class WaterRipplesViewModel : ViewModel() {

    var ripples = mutableStateListOf<Ripple>()
    var counter = MutableStateFlow(0)

    fun addRipple() {
        val color = if (counter.value != 0 && counter.value % 5 == 0) VIVID_ORANGE_COLOR else LIGHT_BLUE_COLOR
        val ripple = Ripple(BUTTON_SIZE.toFloat(), color)
        ripples.add(0,ripple)
        counter.value++
    }

    class Ripple(
        initialSize : Float,
        color : Long,
    ) {
        var size = mutableFloatStateOf(initialSize)
        var color = mutableLongStateOf(color)
        var alpha = mutableFloatStateOf(1f)
    }
}

