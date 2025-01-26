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
import kotlinx.coroutines.withContext
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
        UPLOADING_EVENTS,
        ERROR,
        ALREADY_CONNECTED
    }

    private var model = MainModel(viewModelScope)

    // ================================================================================ |
    // ================================ STATE FIELDS ================================== |
    // ================================================================================ |

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

    private var _gameDuration = MutableStateFlow<Int?>(null)
    val gameDuration : StateFlow<Int?> = _gameDuration

    private var _syncWindowLength = MutableStateFlow<Long?>(null)
    val syncWindowLength : StateFlow<Long?> = _syncWindowLength

    private var _syncTolerance = MutableStateFlow<Long?>(null)
    val syncTolerance : StateFlow<Long?> = _syncTolerance

    private var _ready = MutableStateFlow(false)
    val ready : StateFlow<Boolean> = _ready

    private var _timeServerStartTime = MutableStateFlow(-1L)
    val gameStartTime : StateFlow<Long> = _timeServerStartTime

    private var _additionalData = MutableStateFlow("")
    val additionalData : StateFlow<String> = _additionalData

    private var sessionId = -1
    var temporaryPlayerId = ""

    // ================================================================================ |
    // ============================ PUBLIC METHODS ==================================== |
    // ================================================================================ |


    fun connect() {
        viewModelScope.launch(Dispatchers.IO){
            setState(State.CONNECTING)
            while(true){
                if(model.connectToServer()){
                    break
                }
            }
            setState(State.SELECTING_ID)
        }
    }

    fun enter(selectedId: String, force : Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            setState(State.CONNECTING)
            while (true) {
                val playerId: String?
                try{
                    playerId = model.enter(selectedId,force)
                } catch (_: MainModel.AlreadyConnectedException){
                    setState(State.ALREADY_CONNECTED)
                    temporaryPlayerId = selectedId
                    return@launch
                }
                if (playerId != null) {
                    _playerId.value = playerId
                    setState(State.CONNECTED_NOT_IN_LOBBY)
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
            setState(State.SELECTING_ID)
        }
    }

    fun afterGame(result: Result) {
        Log.d(TAG, "afterGame: $result")
        viewModelScope.launch(Dispatchers.Default) {
            _ready.value = false
            _timeServerStartTime.value = -1
            _gameType.value = null
            _gameDuration.value = null
            when (result.code) {
                Result.Code.OK -> {
                    setState(State.UPLOADING_EVENTS)
                    withContext(Dispatchers.IO) {
                        do {
                            if (model.uploadSessionEvents(sessionId)) {
                                break
                            }
                        } while (true)
                        sessionId = -1
                    }
                    setState(State.CONNECTED_IN_LOBBY)
                }

                else -> {
                    // typically, when reaching here, the game ended due to a network error
                    // or some other issue that hasn't been discovered yet.
                    // so we want to display an error message to the user
                    // and restart the application.
                    val error =
                        "${result.code.prettyName()}:\n${result.errorMessage ?: "no error message"}"
                    fatalError(error)
                }
            }
            setupListeners() // take back control of the listeners
        }
    }

    fun clearError() {
        _error.value = null

        if(_lobbyId.value.isNotEmpty()){
            // if there is a lobbyId, then we're connected and in a lobby
            setState(State.CONNECTED_IN_LOBBY)
        } else if(_playerId.value.isNotEmpty()){
            // if there is only a playerId, then we're connected but not in a lobby
            setState(State.CONNECTED_NOT_IN_LOBBY)
        } else {
            // if there is no playerId, then we're disconnected
            setState(State.DISCONNECTED)
        }
    }

    fun showError(string: String) {
        _error.value = if(_error.value != null){
            string + "\n- - - - - - -\n" + _error.value
        } else {
            string
        }
        setState(State.ERROR)
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

    fun setState(newState: State){
        _state.value = newState
        Log.d(TAG, "set new state: $newState")
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

                // reset data just in case
                _gameType.value = null
                _gameDuration.value = null
                _ready.value = false

                _lobbyId.value = lobbyId
                setState(State.CONNECTED_IN_LOBBY)
            }
            GameRequest.Type.LEAVE_LOBBY -> {
                _lobbyId.value = ""
                setState(State.CONNECTED_NOT_IN_LOBBY)
            }
            GameRequest.Type.CONFIGURE_LOBBY -> {
                val gameType = request.gameType ?: run {
                    Log.e(TAG, "handleGameRequest: CONFIGURE_LOBBY request missing gameType")
                    showError("Failed to configure lobby")
                    return
                }
                val gameDuration = request.duration ?: run {
                    Log.e(TAG, "handleGameRequest: CONFIGURE_LOBBY request missing duration")
                    showError("Failed to configure lobby")
                    return
                }
                val syncWindowLength = request.syncWindowLength ?: run {
                    Log.e(TAG, "handleGameRequest: CONFIGURE_LOBBY request missing syncWindowLength")
                    showError("Failed to configure lobby")
                    return
                }
                val syncTolerance = request.syncTolerance ?: run {
                    Log.e(TAG, "handleGameRequest: CONFIGURE_LOBBY request missing syncTolerance")
                    showError("Failed to configure lobby")
                    return
                }

                _gameType.value = gameType
                _gameDuration.value = gameDuration
                _syncWindowLength.value = syncWindowLength
                _syncTolerance.value = syncTolerance
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

                sessionId = request.sessionId?.toInt() ?: run{
                    Log.e(TAG, "handleGameRequest: START_GAME request missing sessionId")
                    fatalError("Failed to start game: missing session id")
                    return
                }
                _timeServerStartTime.value = request.timestamp?.toLong() ?: run{
                    Log.e(TAG, "handleGameRequest: START_GAME request missing start time")
                    fatalError("Failed to start game: missing start time")
                    return
                }
                _additionalData.value = request.data?.joinToString(";") ?: ""

                setState(State.IN_GAME)
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