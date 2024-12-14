package com.imsproject.watch.viewmodel

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imsproject.common.gameServer.GameAction
import com.imsproject.common.gameServer.GameRequest
import com.imsproject.common.gameServer.GameRequest.Type
import com.imsproject.common.gameServer.GameType
import com.imsproject.watch.PACKAGE_PREFIX
import com.imsproject.watch.model.MainModel
import com.imsproject.watch.view.contracts.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.java_websocket.exceptions.WebsocketNotConnectedException

private const val TAG = "MainViewModel"

class MainViewModel() : ViewModel() {

    enum class State {
        DISCONNECTED,
        CONNECTING,
        CONNECTED_NOT_IN_LOBBY,
        CONNECTED_IN_LOBBY,
        IN_GAME,
        ERROR
    }

    // ================================================================================ |
    // ================================ STATE FIELDS ================================== |
    // ================================================================================ |

    private val model = MainModel(viewModelScope)

    private var _state = MutableStateFlow(State.DISCONNECTED)
    val state : StateFlow<State> = _state

    private var _playerId = MutableStateFlow("")
    val playerId : StateFlow<String> = _playerId

    private var _error = MutableStateFlow<String?>(null)
    val error : StateFlow<String?> = _error

    private var _lobbyId = MutableStateFlow("")
    val lobbyId : StateFlow<String> = _lobbyId

    private var _gameType = MutableStateFlow<GameType?>(null)
    val gameType : StateFlow<GameType?> = _gameType

    private var _ready = MutableStateFlow(false)
    val ready : StateFlow<Boolean> = _ready

    // ================================================================================ |
    // ============================ PUBLIC METHODS ==================================== |
    // ================================================================================ |

    fun connect() {
        viewModelScope.launch(Dispatchers.IO){
            _state.value = State.CONNECTING
            val id = model.connectToServer()
            if (id != null) {
                _playerId.value = id
                _state.value = State.CONNECTED_NOT_IN_LOBBY
                setupListeners()
            } else {
                showError("Failed to connect to server")
            }
        }
    }

    fun afterGame(result: Result) {
        setupListeners()
        _ready.value = false
        when(result.code){
            Result.Code.OK -> _state.value = State.CONNECTED_IN_LOBBY
            else -> {
                val error = "${result.code.prettyName()}:\n${result.errorMessage ?: "no error message"}"
                fatalError(error)
            }
        }
    }

    fun clearError() {
        _error.value = null

        if(_lobbyId.value.isNotEmpty()){
            _state.value = State.CONNECTED_IN_LOBBY
        } else if(_playerId.value.isNotEmpty()){
            _state.value = State.CONNECTED_NOT_IN_LOBBY
        } else {
            _state.value = State.DISCONNECTED
        }
    }

    fun showError(string: String) {
        _error.value = string
        _state.value = State.ERROR
    }

    fun toggleReady() {
        viewModelScope.launch(Dispatchers.IO) {
            model.toggleReady()
            _ready.value = !_ready.value
        }
    }

    // ================================================================================ |
    // ============================ PRIVATE METHODS =================================== |
    // ================================================================================ |

    private fun handleGameRequest(request: GameRequest){
        when (request.type){
            Type.HEARTBEAT -> {}
            Type.JOIN_LOBBY -> {
                val lobbyId = request.lobbyId ?: run {
                    Log.e(TAG, "handleGameRequest: JOIN_LOBBY request missing lobbyId")
                    showError("Failed to join lobby")
                    return
                }
                val gameType = request.gameType ?: run {
                    Log.e(TAG, "handleGameRequest: JOIN_LOBBY request missing gameType")
                    showError("Failed to join lobby")
                    return
                }

                _gameType.value = gameType
                _lobbyId.value = lobbyId
                _state.value = State.CONNECTED_IN_LOBBY
            }
            Type.LEAVE_LOBBY -> {
                _lobbyId.value = ""
                _state.value = State.CONNECTED_NOT_IN_LOBBY
            }
            Type.START_GAME -> {
                clearListeners()
                _state.value = State.IN_GAME
            }
            else -> {
                Log.e(TAG, "handleGameRequest: Unexpected request type: ${request.type}")
                val errorMsg = "Unexpected request type received\n" +
                        "request type: ${request.type}\n"+
                        "request content:\n$request"
                showError(errorMsg)
            }
        }
    }

    private fun handleGameAction(action: GameAction){
        when(action.type){
            GameAction.Type.HEARTBEAT -> {}
            else -> {
                Log.e(TAG, "handleGameAction: Unexpected action type: ${action.type}")
                val errorMsg = "Unexpected request type received\n" +
                        "request type: ${action.type}\n"+
                        "request content:\n$action"
                showError(errorMsg)
            }
        }
    }

    private fun setupListeners() {
        model.onTcpMessage({ handleGameRequest(it) }) {
            Log.e(TAG, "tcp exception", it)
            if(it is WebsocketNotConnectedException){
                fatalError("Connection lost")
            } else {
                showError(it.message ?: it.cause?.message ?: "unknown tcp exception")
            }
        }
        model.onTcpError {
            Log.e(TAG, "tcp error", it)
            showError(it.message ?: it.cause?.message ?: "unknown tcp error")
        }
        model.onUdpMessage({ handleGameAction(it) }) {
            Log.e(TAG, "udp exception", it)
            showError(it.message ?: it.cause?.message ?: "unknown udp exception")
        }
    }

    private fun clearListeners() {
        model.onTcpMessage(null)
        model.onTcpError(null)
        model.onUdpMessage(null)
    }

    private fun fatalError(message: String) {
        _playerId.value = ""
        _lobbyId.value = ""
        _gameType.value = null
        showError("The application encountered a fatal error and must be restarted.\n$message")
    }
}