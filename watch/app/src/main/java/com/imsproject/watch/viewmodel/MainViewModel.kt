package com.imsproject.watch.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imsproject.common.gameserver.GameAction
import com.imsproject.common.gameserver.GameRequest
import com.imsproject.common.gameserver.GameType
import com.imsproject.watch.model.MainModel
import com.imsproject.watch.view.contracts.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.java_websocket.exceptions.WebsocketNotConnectedException

private const val TAG = "MainViewModel"

class MainViewModel() : ViewModel() {

    enum class State {
        DISCONNECTED,
        SELECTING_ID,
        CONNECTING,
        CONNECTED_NOT_IN_LOBBY,
        CONNECTED_IN_LOBBY,
        IN_GAME,
        ERROR
    }

    private var model = MainModel(viewModelScope)

    // ================================================================================ |
    // ================================ STATE FIELDS ================================== |
    // ================================================================================ |

    private var _state = MutableStateFlow(State.DISCONNECTED)
        set(value) {
            field = value
            Log.d(TAG, "state changed to: $value")
        }
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

    private var _timeServerStartTime = MutableStateFlow(-1L)
    val gameStartTime : StateFlow<Long> = _timeServerStartTime

    private var _additionalData = MutableStateFlow("")
    val additionalData : StateFlow<String> = _additionalData

    // ================================================================================ |
    // ============================ PUBLIC METHODS ==================================== |
    // ================================================================================ |


    fun connect() {
        viewModelScope.launch(Dispatchers.IO){
            _state.value = State.CONNECTING
            while(true){
                if(model.connectToServer()){
                    break
                }
            }
            _state.value = State.SELECTING_ID
        }
    }

    fun enter(selectedId: String? = null){
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = State.CONNECTING
            while (true) {
                val playerId = model.enter(selectedId)
                if (playerId != null) {
                    _playerId.value = playerId
                    _state.value = State.CONNECTED_NOT_IN_LOBBY
                    setupListeners() // setup the listeners to start receiving messages
                    return@launch
                }
            }
        }
    }

    fun exit() {
        viewModelScope.launch(Dispatchers.IO) {
            if(_state.value != State.CONNECTED_NOT_IN_LOBBY){
                showError("Cannot exit at the current state")
                return@launch
            }
            model.exit()
            _playerId.value = ""
            _timeServerStartTime.value = -1
            _state.value = State.SELECTING_ID
        }
    }

    fun afterGame(result: Result) {
        setupListeners() // take back control of the listeners
        _ready.value = false
        when(result.code){
            Result.Code.OK ->{
                _timeServerStartTime.value = -1
                _state.value = State.CONNECTED_IN_LOBBY
            }
            else -> {
                // typically, when reaching here, the game ended due to a network error
                // or some other issue that hasn't been discovered yet.
                // so we want to display an error message to the user
                // and restart the application.
                val error = "${result.code.prettyName()}:\n${result.errorMessage ?: "no error message"}"
                fatalError(error)
            }
        }
    }

    fun clearError() {
        _error.value = null

        if(_lobbyId.value.isNotEmpty()){
            // if there is a lobbyId, then we're connected and in a lobby
            _state.value = State.CONNECTED_IN_LOBBY
        } else if(_playerId.value.isNotEmpty()){
            // if there is only a playerId, then we're connected but not in a lobby
            _state.value = State.CONNECTED_NOT_IN_LOBBY
        } else {
            // if there is no playerId, then we're disconnected
            _state.value = State.DISCONNECTED
        }
    }

    fun showError(string: String) {
        _error.value = if(_error.value != null){
            string + "\n- - - - - - -\n" + _error.value
        } else {
            string
        }
        _state.value = State.ERROR
    }

    fun toggleReady() {
        viewModelScope.launch(Dispatchers.IO) {
            model.toggleReady()
            _ready.value = !_ready.value
        }
    }

    fun onDestroy() {
        runBlocking(Dispatchers.Main){
            model.closeAllResources()
        }
    }

    // ================================================================================ |
    // ============================ PRIVATE METHODS =================================== |
    // ================================================================================ |

    private suspend fun handleGameRequest(request: GameRequest){
        when (request.type){
            GameRequest.Type.PONG -> {}
            GameRequest.Type.HEARTBEAT -> {}
            GameRequest.Type.END_GAME -> {}
            GameRequest.Type.EXIT -> {
                fatalError(request.message ?: "Connection closed by server")
            }
            GameRequest.Type.JOIN_LOBBY -> {
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
            GameRequest.Type.LEAVE_LOBBY -> {
                _lobbyId.value = ""
                _state.value = State.CONNECTED_NOT_IN_LOBBY
            }
            GameRequest.Type.START_GAME -> {
                if(_state.value != State.CONNECTED_IN_LOBBY){
                    Log.e(TAG, "handleGameRequest: START_GAME request received while not in lobby")
                    return
                }
                // ===================================|
                // clear the listeners to prevent any further messages from being processed.
                // let the game activity handle the messages from here on out.
                /*(!)*/ clearListeners()
                // ===================================|
                _timeServerStartTime.value = request.timestamp?.toLong() ?: run{
                    Log.e(TAG, "handleGameRequest: START_GAME request missing start time")
                    fatalError("Failed to start game: missing start time")
                    return
                }
                _additionalData.value = request.data?.joinToString(";") ?: ""

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
            GameAction.Type.PONG -> {}
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
        val oldModel = model
        _playerId.value = ""
        _lobbyId.value = ""
        _gameType.value = null
        _timeServerStartTime.value = -1
        showError(message)
        viewModelScope.launch(Dispatchers.IO) {
            oldModel.closeAllResources()
        }
        model = MainModel(viewModelScope)
    }
}