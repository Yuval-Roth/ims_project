package com.imsproject.watch.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imsproject.common.gameServer.GameAction
import com.imsproject.common.gameServer.GameRequest
import com.imsproject.watch.model.MainModel
import com.imsproject.watch.WATER_RIPPLES_BUTTON_SIZE
import com.imsproject.watch.GRAY_COLOR
import com.imsproject.watch.LIGHT_BLUE_COLOR
import com.imsproject.watch.VIVID_ORANGE_COLOR
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

class WaterRipplesViewModel() : ViewModel() {

//    init {
//        viewModelScope.launch{
//            while(true){
//                delay(1000)
//                showRipple("other",false)
//            }
//        }
//    }

    val model = MainModel.instance
    val playerId : String = model.playerId ?: run {
        Log.e(TAG, "init: missing player ID")
        "unknown player ID"
    }
    var ripples = mutableStateListOf<Ripple>()
    var counter = MutableStateFlow(0)

    var _playing = MutableStateFlow(true)
    val playing : StateFlow<Boolean> = _playing

    var _error = MutableStateFlow<String?>(null)
    val error : StateFlow<String?> = _error

    fun onCreate(){
        model.onTcpMessage({handleGameRequest(it)}){
            Log.e(TAG, "tcp exception", it)
            showError(it.message?: "unknown tcp exception")
        }
        model.onTcpError {
            Log.e(TAG, "tcp error", it)
            showError(it.message?: "unknown tcp error")
        }
        model.onUdpMessage( { handleGameAction(it) }){
            Log.e(TAG, "udp error", it)
            showError(it.message?: "unknown udp error")
        }
    }

    fun onFinish(){
        model.onTcpMessage(null)
        model.onTcpError(null)
        model.onUdpMessage(null)
    }

    private fun handleGameAction(action: GameAction) {
        when (action.type) {
            GameAction.Type.CLICK -> {
                val actor = action.actor ?: run{
                    Log.e(TAG, "handleGameAction: missing actor in click action")
                    return
                }
                showRipple(actor)
            }
            else -> {
                Log.e(TAG, "handleGameAction: invalid action type: ${action.type}")
            }
        }
    }

    private fun handleGameRequest(request: GameRequest) {
        when (request.type) {
            GameRequest.Type.END_GAME -> {
                _playing.value = false
            }
            else -> {
                Log.e(TAG, "handleGameRequest: invalid request type: ${request.type}")
            }
        }
    }

    fun click() {
//        model.sendClick() // TODO: uncomment this line when the server is ready
        showRipple(playerId) // TODO: remove this line when the server is ready
    }

    // TODO, remove the inSync default value when the server is ready
    fun showRipple(actor: String, inSync : Boolean = counter.value != 0 && counter.value % 5 == 0) {

        val ripple = if (inSync){
            Ripple(VIVID_ORANGE_COLOR)
        } else if(actor == playerId){
            Ripple(LIGHT_BLUE_COLOR)
        } else  {
            Ripple(GRAY_COLOR,0.5f)
        }
        ripples.add(0,ripple)
        counter.value++
    }

    class Ripple(
        color: Color,
        val startingAlpha: Float = 1f,
    ) {
        var color = mutableStateOf(color)
        var size = mutableFloatStateOf(WATER_RIPPLES_BUTTON_SIZE.toFloat())
        var currentAlpha = mutableFloatStateOf(startingAlpha)
    }

    fun showError(string: String) {
        _error.value = string
    }

    fun clearError() {
        _error.value = null
    }

    companion object {
        private const val TAG = "WaterRipplesViewModel"
    }
}

