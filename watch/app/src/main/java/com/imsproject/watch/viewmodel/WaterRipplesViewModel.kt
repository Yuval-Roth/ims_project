package com.imsproject.watch.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableFloatStateOf
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
import com.imsproject.watch.view.contracts.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.java_websocket.exceptions.WebsocketNotConnectedException

class WaterRipplesViewModel() : ViewModel() {

    class Ripple(
        color: Color,
        val startingAlpha: Float = 1f,
    ) {
        var color = mutableStateOf(color)
        var size = mutableFloatStateOf(WATER_RIPPLES_BUTTON_SIZE.toFloat())
        var currentAlpha = mutableFloatStateOf(startingAlpha)
    }

    val model = MainModel.instance
    val playerId : String = model.playerId ?: run {
        Log.e(TAG, "init: missing player ID")
        "unknown player ID"
    }

    // ================================================================================ |
    // ================================ STATE FIELDS ================================== |
    // ================================================================================ |

    var ripples = mutableStateListOf<Ripple>()
    var counter = MutableStateFlow(0)

    var _playing = MutableStateFlow(true)
    val playing : StateFlow<Boolean> = _playing

    var _error = MutableStateFlow<String?>(null)
    val error : StateFlow<String?> = _error

    var _resultCode = MutableStateFlow(Result.Code.OK)
    val resultCode : StateFlow<Result.Code> = _resultCode

    // ================================================================================ |
    // ============================ PUBLIC METHODS ==================================== |
    // ================================================================================ |

    fun onCreate(){
        setupListeners()
    }

    fun click() {
        viewModelScope.launch(Dispatchers.IO) {
            model.sendClick()
        }
    }

    // ================================================================================ |
    // ============================ PRIVATE METHODS =================================== |
    // ================================================================================ |

    private suspend fun handleGameAction(action: GameAction) {
        when (action.type) {
            GameAction.Type.HEARTBEAT -> {}
            GameAction.Type.CLICK -> {
                val actor = action.actor ?: run{
                    Log.e(TAG, "handleGameAction: missing actor in click action")
                    return
                }
                val inSync = action.inSync ?: run{
                    Log.e(TAG, "handleGameAction: missing inSync in click action")
                    return
                }
                // switch to main thread to update UI
                withContext(Dispatchers.Main) {
                    showRipple(actor, inSync)
                }
            }
            else -> {
                Log.e(TAG, "handleGameRequest: Unexpected action type: ${action.type}")
                val errorMsg = "Unexpected request type received\n" +
                        "request type: ${action.type}\n"+
                        "request content:\n$action"
                exitWithError(errorMsg,Result.Code.UNEXPECTED_REQUEST)
            }
        }
    }

    private fun handleGameRequest(request: GameRequest) {
        when (request.type) {
            GameRequest.Type.HEARTBEAT -> {}
            GameRequest.Type.END_GAME -> exitOk()
            else -> {
                Log.e(TAG, "handleGameRequest: Unexpected request type: ${request.type}")
                val errorMsg = "Unexpected request type received\n" +
                        "request type: ${request.type}\n"+
                        "request content:\n$request"
                exitWithError(errorMsg,Result.Code.UNEXPECTED_REQUEST)
            }
        }
    }

    private fun setupListeners() {
        model.onTcpMessage({ handleGameRequest(it) }) {
            Log.e(TAG, "tcp exception", it)
            if(it is WebsocketNotConnectedException){
                exitWithError("Connection lost",Result.Code.CONNECTION_LOST)
            } else {
                exitWithError(it.message ?: it.cause?.message ?: "unknown tcp exception",Result.Code.TCP_EXCEPTION)
            }
        }
        model.onTcpError {
            Log.e(TAG, "tcp error", it)
            exitWithError(it.message ?: it.cause?.message ?: "unknown tcp error",Result.Code.TCP_ERROR)
        }
        model.onUdpMessage({ handleGameAction(it) }) {
            Log.e(TAG, "udp exception", it)
            exitWithError(it.message ?: it.cause?.message ?: "unknown udp exception",Result.Code.UDP_EXCEPTION)
        }
    }

    private fun clearListeners(){
        model.onTcpMessage(null)
        model.onTcpError(null)
        model.onUdpMessage(null)
    }

    private fun showRipple(actor: String, inSync : Boolean) {

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

    private fun exitWithError(string: String, code: Result.Code) {
        clearListeners()
        _error.value = string
        _resultCode.value = code
        _playing.value = false
    }

    private fun exitOk() {
        clearListeners()
        _playing.value = false
    }

    companion object {
        private const val TAG = "WaterRipplesViewModel"
    }
}

