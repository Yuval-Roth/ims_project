package com.imsproject.watch.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.imsproject.common.gameserver.GameType
import com.imsproject.watch.model.MainModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TutorialViewModel: GameViewModel(GameType.TUTORIAL) {

    private val _currentTutorial = MutableStateFlow(GameType.WATER_RIPPLES)
    val currentTutorial: StateFlow<GameType> = _currentTutorial

    override fun onCreate(intent: Intent, context: Context) {
        tutorialMode()
        MainModel(viewModelScope) // TODO: remove this when the MainModel is created in the MainActivity
        super.onCreate(intent,context)
        setupListeners()
    }

    fun nextTutorial() {
        _currentTutorial.value = when (_currentTutorial.value) {
            GameType.WATER_RIPPLES -> GameType.WINE_GLASSES
            GameType.WINE_GLASSES -> GameType.FLOUR_MILL
            GameType.FLOUR_MILL -> GameType.FLOWER_GARDEN
            GameType.FLOWER_GARDEN -> GameType.WATER_RIPPLES
            else -> throw IllegalStateException("Invalid tutorial game type: $currentTutorial")
        }
    }
}