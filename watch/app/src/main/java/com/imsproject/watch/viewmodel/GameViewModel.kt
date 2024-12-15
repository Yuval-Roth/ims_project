package com.imsproject.watch.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.imsproject.common.gameServer.GameAction
import com.imsproject.common.gameServer.GameRequest
import com.imsproject.common.gameServer.GameRequest.Type
import com.imsproject.common.gameServer.GameType
import com.imsproject.watch.model.MainModel
import com.imsproject.watch.view.contracts.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.java_websocket.exceptions.WebsocketNotConnectedException

open class GameViewModel(gameType: GameType) : ViewModel() {

    private val TAG = "$_TAG-${gameType.prettyName()}"
    val model = MainModel.instance
    val playerId : String = model.playerId ?: run {
        Log.e(TAG, "init: missing player ID")
        "unknown player ID"
    }

    // ================================================================================ |
    // ================================ STATE FIELDS ================================== |
    // ================================================================================ |

    protected var _playing = MutableStateFlow(true)
    val playing : StateFlow<Boolean> = _playing

    protected var _error = MutableStateFlow<String?>(null)
    val error : StateFlow<String?> = _error

    protected var _resultCode = MutableStateFlow(Result.Code.OK)
    val resultCode : StateFlow<Result.Code> = _resultCode

    // ================================================================================ |
    // ============================ PUBLIC METHODS ==================================== |
    // ================================================================================ |

    open fun onCreate(){
        setupListeners()
    }

    // ================================================================================ |
    // ============================ PROTECTED METHODS ================================= |
    // ================================================================================ |

    /**
     * handles [GameAction.Type.HEARTBEAT] and everything else is an unexpected request error
     */
    protected open suspend fun handleGameAction(action: GameAction) {
        when (action.type) {
            GameAction.Type.HEARTBEAT -> {}
            else -> {
                Log.e(TAG, "handleGameRequest: Unexpected action type: ${action.type}")
                val errorMsg = "Unexpected request type received\n" +
                        "request type: ${action.type}\n"+
                        "request content:\n$action"
                exitWithError(errorMsg,Result.Code.UNEXPECTED_REQUEST)
            }
        }
    }

    /**
     * handles [GameRequest.Type.HEARTBEAT] and everything else is an unexpected request error
     */
    protected open suspend fun handleGameRequest(request: GameRequest) {
        when (request.type) {
            Type.HEARTBEAT -> {}
            else -> {
                Log.e(TAG, "handleGameRequest: Unexpected request type: ${request.type}")
                val errorMsg = "Unexpected request type received\n" +
                        "request type: ${request.type}\n"+
                        "request content:\n$request"
                exitWithError(errorMsg,Result.Code.UNEXPECTED_REQUEST)
            }
        }
    }

    protected fun exitWithError(string: String, code: Result.Code) {
        clearListeners() // clear the listeners to prevent any further messages from being processed.
        _error.value = string
        _resultCode.value = code
        _playing.value = false
    }

    protected fun exitOk() {
        clearListeners() // clear the listeners to prevent any further messages from being processed.
        _playing.value = false
    }

    // ================================================================================ |
    // ============================ PRIVATE METHODS =================================== |
    // ================================================================================ |

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

    companion object {
        private const val _TAG = "GameViewModel"
    }
}