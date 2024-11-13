package com.imsproject.poc_client.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imsproject.poc_client.model.MainModel
import com.imsproject.utils.PingTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val TAG = "MainViewModel"

class MainViewModel() : ViewModel() {

    enum class State {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        ERROR
    }

    private val model = MainModel()

    private var _state = MutableStateFlow(State.DISCONNECTED)
    val state : StateFlow<State> = _state

    private var _id = MutableStateFlow("")
    val id : StateFlow<String> = _id

    private var _error = MutableStateFlow<String?>(null)
    val error : StateFlow<String?> = _error

    private var _udpPing = MutableStateFlow(-1L)
    val udpPing : StateFlow<Long> = _udpPing

    private var _tcpPing = MutableStateFlow(-1L)
    val tcpPing : StateFlow<Long> = _tcpPing

    private var _udpPacketsLost = MutableStateFlow(0)
    val udpPacketsLost : StateFlow<Int> = _udpPacketsLost

    private var tcpPingJob : Job? = null
    private var udpPingJob : Job? = null

    fun connect() {
        viewModelScope.launch(Dispatchers.IO){
            _state.value = State.CONNECTING
            val id = model.connectToServer()
            if (id != null) {
                _id.value = id
                _state.value = State.CONNECTED
            } else {
                _error.value = "Failed to connect to server"
                _state.value = State.ERROR
            }

            // Start pinging the server
            tcpPingJob = viewModelScope.launch(Dispatchers.IO){
                val tracker = PingTracker()
                tracker.onUpdate = {
                    _tcpPing.value = it
                }
                model.onTcpPong = {
                    tracker.add(it)
                }
                tracker.start()
                while(true){
                    try{
                        model.pingTcp()
                        delay(50)
                    } catch(e: Exception){
                        Log.e(TAG, "Failed to tcp ping server", e)
                    }
                }
            }

            udpPingJob = viewModelScope.launch(Dispatchers.IO){
                val tracker = PingTracker()
                tracker.onUpdate = {
                    _udpPing.value = it
                    _udpPacketsLost.value = tracker.lostCount()
                }
                model.onUdpPong = {
                    tracker.add(it)
                }
                tracker.start()
                while(true) {
                    try {
                        model.pingUdp()
                        tracker.addSent()
                        delay(50)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to udp ping server", e)
                    }
                }
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
}