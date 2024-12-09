package com.imsproject.watch.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imsproject.watch.model.MainModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val TAG = "MainViewModel"

class MainViewModel() : ViewModel() {

    enum class State {
        DISCONNECTED,
        CONNECTING,
        CONNECTED_NOT_IN_LOBBY,
        CONNECTED_IN_LOBBY,
        ERROR
    }

    private val model = MainModel(viewModelScope)

    private var _state = MutableStateFlow(State.DISCONNECTED)
    val state : StateFlow<State> = _state

    private var _id = MutableStateFlow("")
    val id : StateFlow<String> = _id

    private var _error = MutableStateFlow<String?>(null)
    val error : StateFlow<String?> = _error

    private var _lobbyId = MutableStateFlow("")
    val lobbyId : StateFlow<String> = _lobbyId

    private var _ready = MutableStateFlow(false)
    val ready : StateFlow<Boolean> = _ready

    fun connect() {
        viewModelScope.launch(Dispatchers.IO){
            _state.value = State.CONNECTING
            val id = model.connectToServer()
            if (id != null) {
                _id.value = id
                _state.value = State.CONNECTED_NOT_IN_LOBBY
            } else {
                showError("Failed to connect to server")
            }
        }
    }

    fun clearError() {
        _error.value = null
        _state.value = State.DISCONNECTED
    }

    fun showError(string: String) {
        _error.value = string
        _state.value = State.ERROR
    }

    fun toggleReady() {
        _ready.value = !_ready.value
    }
}