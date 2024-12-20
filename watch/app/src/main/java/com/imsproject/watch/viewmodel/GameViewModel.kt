package com.imsproject.watch.viewmodel

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imsproject.common.etc.TimeRequest
import com.imsproject.common.gameServer.GameAction
import com.imsproject.common.gameServer.GameRequest
import com.imsproject.common.gameServer.GameType
import com.imsproject.watch.PACKAGE_PREFIX
import com.imsproject.watch.model.MainModel
import com.imsproject.watch.view.contracts.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.java_websocket.exceptions.WebsocketNotConnectedException
import kotlinx.coroutines.launch

abstract class GameViewModel(gameType: GameType) : ViewModel() {

    enum class State {
        LOADING,
        PLAYING,
        TERMINATED
    }

    private val TAG = "$_TAG-${gameType.prettyName()}"
    val model = MainModel.instance
    val playerId : String = model.playerId ?: run {
        Log.e(TAG, "init: missing player ID")
        "unknown player ID"
    }

    // ================================================================================ |
    // ================================ STATE FIELDS ================================== |
    // ================================================================================ |

    protected var _state = MutableStateFlow(State.LOADING)
    val state : StateFlow<State> = _state

    protected var _error = MutableStateFlow<String?>(null)
    val error : StateFlow<String?> = _error

    protected var _resultCode = MutableStateFlow(Result.Code.OK)
    val resultCode : StateFlow<Result.Code> = _resultCode

    private var myStartTime = -1L
    private var timeServerStartTime = -1L
    private var gameTimeDelta = 0L

    // ================================================================================ |
    // ============================ PUBLIC METHODS ==================================== |
    // ================================================================================ |

    open fun onCreate(intent: Intent){
        setupListeners()
        myStartTime = intent.getLongExtra("$PACKAGE_PREFIX.myStartTime",-1)
        timeServerStartTime = intent.getLongExtra("$PACKAGE_PREFIX.timeServerStartTime",-1)
        gameTimeDelta = myStartTime - timeServerStartTime
        viewModelScope.launch(Dispatchers.IO) {
            model.sendSyncRequest(timeServerStartTime)
        }
    }

    // ================================================================================ |
    // ============================ PROTECTED METHODS ================================= |
    // ================================================================================ |

    protected fun getCurrentGameTime(): Long {
        return System.currentTimeMillis() - myStartTime - gameTimeDelta
    }

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
            GameRequest.Type.HEARTBEAT -> {}
            GameRequest.Type.SYNC_TIME -> {
                val timestamp = request.data?.get(0)?.toLong() ?: run {
                    Log.e(TAG, "handleGameRequest: missing timestamp in sync time request")
                    return
                }

                // update the time server start time if the new timestamp is earlier
                if(timeServerStartTime > timestamp){
                    timeServerStartTime = timestamp
                    gameTimeDelta = myStartTime - timeServerStartTime
                }

                // After syncing the time we can start the game
                _state.value = State.PLAYING
            }
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
        _state.value = State.TERMINATED
    }

    protected fun exitOk() {
        clearListeners() // clear the listeners to prevent any further messages from being processed.
        _state.value = State.TERMINATED
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