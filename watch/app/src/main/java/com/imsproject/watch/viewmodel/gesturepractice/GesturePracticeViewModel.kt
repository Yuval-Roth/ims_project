package com.imsproject.watch.viewmodel.gesturepractice

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow

interface GesturePracticeViewModel {
    val done: StateFlow<Boolean>
    fun reset()
    @Composable
    fun RunGesturePractice()
}