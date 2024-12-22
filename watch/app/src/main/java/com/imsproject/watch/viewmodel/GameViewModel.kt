package com.imsproject.watch.viewmodel

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imsproject.common.gameServer.GameAction
import com.imsproject.common.gameServer.GameRequest
import com.imsproject.common.gameServer.GameType
import com.imsproject.common.networking.PingTracker
import com.imsproject.watch.PACKAGE_PREFIX
import com.imsproject.watch.model.MainModel
import com.imsproject.watch.view.contracts.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.java_websocket.exceptions.WebsocketNotConnectedException
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

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

    private var timeServerDelta = 0L
    private var myStartTime = 0L
    private var latency = 0L
    private val pingTracker = PingTracker()

    // ================================================================================ |
    // ============================ PUBLIC METHODS ==================================== |
    // ================================================================================ |

    open fun onCreate(intent: Intent){
        setupListeners()

        viewModelScope.launch(Dispatchers.IO) {
            pingTracker.onUpdate = { latency = it }
            pingTracker.start()
            while (true) {
                val now = System.currentTimeMillis()
                model.pingUdp()
                pingTracker.sentAt(now)
                delay(50)
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            delay(1200) // wait for latency to be calculated
            calculateTimeServerDelta()
            println("Time server delta: $timeServerDelta")
            var timeServerStartTime = intent.getLongExtra("$PACKAGE_PREFIX.timeServerStartTime",-1)
            myStartTime = timeServerStartTime + timeServerDelta
            model.sendSyncRequest(getCurrentGameTime() + (latency / 2))
            _state.value = State.PLAYING

//            while(true){
//                val skewedTimestamp = getCurrentGameTime() + (latency / 2)
//                model.sendSyncRequest(skewedTimestamp)
//                delay(100)
//            }
        }
    }

    // ================================================================================ |
    // ============================ PROTECTED METHODS ================================= |
    // ================================================================================ |

    protected fun getCurrentGameTime(): Long {
        return System.currentTimeMillis() - myStartTime
    }

    /**
     * handles [GameAction.Type.HEARTBEAT] and everything else is an unexpected request error
     */
    protected open suspend fun handleGameAction(action: GameAction) {
        when (action.type) {
            GameAction.Type.PONG -> {
                val now = System.currentTimeMillis()
                pingTracker.receivedAt(now)
            }
            GameAction.Type.SYNC_TIME -> {

                val currentGameTime = getCurrentGameTime()

                val timestamp = action.timestamp?.toLong() ?: run {
                    Log.e(TAG, "handleGameAction: missing timestamp in sync time request")
                    return
                }
                val skewedTimestamp = timestamp + (latency / 2)
                val delta = skewedTimestamp - currentGameTime

                // update the time server start time if the new timestamp is earlier
                println("delta: $delta")
                if(delta <= -5){
                    myStartTime -= -5
                }
            }
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

    protected fun calculateTimeServerDelta(){
        val data : List<Long> = List(100) {
            val currentLocal = System.currentTimeMillis()
            val currentTimeServer = model.getTimeServerCurrentTimeMillis()
            currentLocal-currentTimeServer
        }
        timeServerDelta = data.average().toLong()
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