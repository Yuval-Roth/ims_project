package com.imsproject.watch.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imsproject.common.gameServer.GameRequest
import com.imsproject.common.gameServer.GameRequest.Type
import com.imsproject.common.gameServer.GameType
import com.imsproject.watch.model.MainModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

private const val TAG = "MainViewModel"

class MainViewModel() : ViewModel() {

    init{
        viewModelScope.launch(Dispatchers.IO){
            delay(5000)
            val joinLobby = GameRequest.builder(Type.JOIN_LOBBY)
                .lobbyId("lobby1")
                .gameType(GameType.WATER_RIPPLES)
                .build()
            handleGameRequest(joinLobby)
            while(_ready.value == false){
                delay(1000)
            }
            delay(1000)
            val startGame = GameRequest.builder(Type.START_GAME).build()
            handleGameRequest(startGame)
        }
    }

    enum class State {
        DISCONNECTED,
        CONNECTING,
        CONNECTED_NOT_IN_LOBBY,
        CONNECTED_IN_LOBBY,
        IN_GAME,
        ERROR
    }

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

    fun connect() {
        viewModelScope.launch(Dispatchers.IO){
            _state.value = State.CONNECTING
            val id = model.connectToServer()
            if (id != null) {
                _playerId.value = id
                _state.value = State.CONNECTED_NOT_IN_LOBBY
                model.onTcpMessage({handleGameRequest(it)})
            } else {
                showError("Failed to connect to server")
            }
        }
    }

    private fun handleGameRequest(request: GameRequest){
        when (request.type){
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
                _state.value = State.IN_GAME
            }
            else -> {
                Log.e(TAG, "handleGameRequest: Unexpected request type: ${request.type}")
                showError("Unexpected request type received")
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
        model.toggleReady()
        _ready.value = !_ready.value
    }

    fun launchGame() {
        _state.value = State.IN_GAME
    }

    fun afterGame(){
        _state.value = State.CONNECTED_NOT_IN_LOBBY
    }
}