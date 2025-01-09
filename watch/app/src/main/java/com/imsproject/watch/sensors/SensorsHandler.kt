package com.imsproject.watch.sensors

import android.content.Context
import android.se.omapi.Session
import com.imsproject.common.gameserver.SessionEvent
import com.imsproject.watch.viewmodel.GameViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers

class SensorsHandler(
    val scope: CoroutineScope,
    val context: Context,
    val gameViewModel: GameViewModel
) {
    fun run() {
        scope.launch(Dispatchers.IO){
            // do everything here

            // example
            gameViewModel.addEvent(
                SessionEvent.bloodOxygen(
                    gameViewModel.playerId,
                    gameViewModel.getCurrentGameTime(),
                    98
                )
            )
        }
    }
}