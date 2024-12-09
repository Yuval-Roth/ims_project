package com.imsproject.watch.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class WaterRipplesViewModel : ViewModel() {

    var ripples = mutableStateListOf<Boolean>()

    fun addRipple() {
        val inSync = ripples.size % 5 == 0
        ripples.add(0,inSync)
    }
}