package com.imsproject.watch.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Vibrator
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imsproject.common.gameserver.GameAction
import com.imsproject.common.gameserver.GameRequest
import com.imsproject.common.gameserver.GameType
import com.imsproject.common.gameserver.SessionEvent
import com.imsproject.watch.ACTIVITY_DEBUG_MODE
import com.imsproject.watch.PACKAGE_PREFIX
import com.imsproject.watch.model.MainModel
import com.imsproject.watch.model.SessionEventCollector
import com.imsproject.watch.model.SessionEventCollectorImpl
import com.imsproject.watch.sensors.SensorsHandler
import com.imsproject.watch.utils.LatencyTracker
import com.imsproject.watch.utils.PacketTracker
import com.imsproject.watch.view.contracts.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.java_websocket.exceptions.WebsocketNotConnectedException
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext


abstract class GameViewModel(
    gameType: GameType
) : ViewModel(), SessionEventCollector by SessionEventCollectorImpl.getInstance() {

    enum class State {
        LOADING,
        PLAYING,
        TRYING_TO_RECONNECT,
        ERROR,
        TERMINATED
    }

    private val TAG = "$_TAG-${gameType.prettyName()}"
    val model = if(ACTIVITY_DEBUG_MODE) MainModel(viewModelScope) else MainModel.instance
    val playerId : String = model.playerId ?: run {
        Log.e(TAG, "init: missing player ID")
        "unknown player ID"
    }

    protected lateinit var vibrator: Vibrator
        private set
    protected val packetTracker = PacketTracker()
    private lateinit var latencyTracker : LatencyTracker
    private lateinit var sensorsHandler: SensorsHandler

    // ================================================================================ |
    // ================================ STATE FIELDS ================================== |
    // ================================================================================ |

    private var _state = MutableStateFlow(State.LOADING)
        set(value){
            field = value
            Log.d(TAG, "state changed to $value")
        }
    val state : StateFlow<State> = _state

    private var _error = MutableStateFlow<String?>(null)
    val error : StateFlow<String?> = _error

    private var _resultCode = MutableStateFlow(Result.Code.OK)
    val resultCode : StateFlow<Result.Code> = _resultCode

    private var timeServerDelta = 0L
    private var myStartTime = 0L

    // ================================================================================ |
    // ============================ PUBLIC METHODS ==================================== |
    // ================================================================================ |

    open fun onCreate(intent: Intent, context: Context){

        vibrator = context.getSystemService(Vibrator::class.java)

        if(ACTIVITY_DEBUG_MODE){
            _state.value = State.PLAYING
            return
        }

        // set up everything required for the session
        viewModelScope.launch(Dispatchers.Default) {

            // =================== clock synchronization =================== |

            val timeServerStartTime = intent.getLongExtra("$PACKAGE_PREFIX.timeServerStartTime",-1)
            withContext(Dispatchers.IO){
                timeServerDelta = model.calculateTimeServerDelta()
            }
            myStartTime = timeServerStartTime + timeServerDelta

            // log metadata
            val timestamp = getCurrentGameTime()
            addEvent(SessionEvent.serverStartTime(playerId, timestamp,timeServerStartTime.toString()))
            addEvent(SessionEvent.timeServerDelta(playerId,timestamp,timeServerDelta.toString()))
            addEvent(SessionEvent.clientStartTime(playerId,timestamp,myStartTime.toString()))

            // =================== other setup =================== |

            setupListeners()

            packetTracker.onOutOfOrderPacket = {
                addEvent(SessionEvent.packetOutOfOrder(playerId,getCurrentGameTime()))
            }

            sensorsHandler = SensorsHandler(viewModelScope,context,this@GameViewModel)
            sensorsHandler.run()

            latencyTracker = model.getLatencyTracker()
            latencyTracker.onReceive = {
                addEvent(SessionEvent.latency(playerId,getCurrentGameTime(),it.toString()))
            }
            latencyTracker.onTimeout = {
                addEvent(SessionEvent.timedOut(playerId,getCurrentGameTime()))
            }

            // start collecting latency statistics
            latencyTracker.start()
            viewModelScope.launch(Dispatchers.Default) {
                while(true){
                    delay(1000)
                    @Suppress("NAME_SHADOWING")
                    val timestamp = getCurrentGameTime()
                    val statistics = latencyTracker.collectStatistics()
                    addEvent(SessionEvent.averageLatency(playerId,timestamp,statistics.averageLatency.toString()))
                    addEvent(SessionEvent.minLatency(playerId,timestamp,statistics.minLatency.toString()))
                    addEvent(SessionEvent.maxLatency(playerId,timestamp,statistics.maxLatency.toString()))
                    addEvent(SessionEvent.jitter(playerId,timestamp,statistics.jitter.toString()))
                    addEvent(SessionEvent.medianLatency(playerId,timestamp,statistics.median.toString()))
                    addEvent(SessionEvent.measurementCount(playerId,timestamp,statistics.measurementCount.toString()))
                    addEvent(SessionEvent.timeoutThreshold(playerId,timestamp,statistics.timeoutThreshold.toString()))
                    addEvent(SessionEvent.timeoutsCount(playerId,timestamp,statistics.timeoutsCount.toString()))
                }
            }

            // =================== game start =================== |
            addEvent(SessionEvent.sessionStarted(playerId,getCurrentGameTime()))
            Log.d(TAG, "onCreate: session started")
            _state.value = State.PLAYING
        }

    }

    fun exitWithError(errorMessage: String, code: Result.Code) {
        Log.d(TAG, "exitWithError: game ended with code: $code and error: $errorMessage")
        clearListeners() // clear the listeners to prevent any further messages from being processed.
        addEvent(SessionEvent.sessionEnded(playerId,getCurrentGameTime(),errorMessage))
        _error.value = errorMessage
        _resultCode.value = code
        _state.value = State.TERMINATED
    }

    fun getCurrentGameTime(): Long {
        return System.currentTimeMillis() - myStartTime
    }

    fun showError(msg: String) {
        Log.d(TAG, "showError: $msg")
        _error.value = msg
        _state.value = State.ERROR
    }

    fun clearError() {
        if(_state.value == State.ERROR){
            Log.d(TAG, "clearError: clearing error")
            _error.value = null
            _state.value = State.PLAYING
        }
    }

    // ================================================================================ |
    // ============================ PROTECTED METHODS ================================= |
    // ================================================================================ |

    protected open suspend fun handleGameAction(action: GameAction) {
        when (action.type) {
            GameAction.Type.PONG -> {}
            GameAction.Type.HEARTBEAT -> {}
            else -> {
                Log.e(TAG, "handleGameRequest: Unexpected action type: ${action.type}")
                val errorMsg = "Unexpected request type received\n" +
                        "request type: ${action.type}\n"+
                        "request content:\n$action"
                showError(errorMsg)
            }
        }
    }

    protected open suspend fun handleGameRequest(request: GameRequest) {
        when (request.type) {
            GameRequest.Type.HEARTBEAT -> {}
            GameRequest.Type.EXIT -> {
                val errorMessage = request.message ?: ""
                exitWithError(errorMessage,Result.Code.SERVER_CLOSED_CONNECTION)
            }
            GameRequest.Type.END_GAME -> {
                val success = request.success ?: run {
                    Log.e(TAG, "handleGameRequest: missing success field in END_GAME request")
                    return
                }

                if(success){
                    exitOk()
                } else {
                    val errorMessage = request.message ?: "Unknown error"
                    exitWithError(errorMessage,Result.Code.GAME_ENDED_WITH_ERROR)
                }
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

    protected fun exitOk() {
        Log.d(TAG, "exitOk: game ended successfully")
        addEvent(SessionEvent.sessionEnded(playerId,getCurrentGameTime(),"ok"))
        clearListeners() // clear the listeners to prevent any further messages from being processed.
        _state.value = State.TERMINATED
    }

    // ================================================================================ |
    // ============================ PRIVATE METHODS =================================== |
    // ================================================================================ |

    private fun reconnect(onFailure: () -> Unit) {
        _state.value = State.TRYING_TO_RECONNECT
        viewModelScope.launch(Dispatchers.IO) {
            val timeout = System.currentTimeMillis() + 10000
            var reconnected = false
            model.closeAllResources()
            while(!reconnected && System.currentTimeMillis() < timeout){
                if(model.connectToServer()){
                    if(model.reconnect()){
                        reconnected = true
                    }
                }
                Log.d(TAG, "reconnect: reconnected = $reconnected")
            }
            if(reconnected){
                addEvent(SessionEvent.reconnected(playerId,getCurrentGameTime()))
                setupListeners()
                _state.value = State.PLAYING
            } else {
                onFailure()
            }
        }
    }

    private fun setupListeners() {
        model.onTcpMessage({ handleGameRequest(it) }) {
            Log.e(TAG, "tcp exception", it)
            if(it is WebsocketNotConnectedException){
                addEvent(SessionEvent.networkError(playerId,getCurrentGameTime(),"Connection lost"))
                reconnect {
                    exitWithError("Connection lost", Result.Code.CONNECTION_LOST)
                }
            } else {
                val errorMessage = it.message ?: it.cause?.message ?: "unknown tcp exception"
                addEvent(SessionEvent.networkError(playerId,getCurrentGameTime(),errorMessage))
                reconnect {
                    exitWithError(errorMessage, Result.Code.TCP_EXCEPTION)
                }
            }
        }
        model.onTcpError {
            Log.e(TAG, "tcp error", it)
            val errorMessage = it.message ?: it.cause?.message ?: "unknown tcp error"
            addEvent(SessionEvent.networkError(playerId,getCurrentGameTime(),errorMessage))
            reconnect {
                exitWithError(errorMessage, Result.Code.TCP_ERROR)
            }
        }
        model.onUdpMessage({ handleGameAction(it) }) {
            Log.e(TAG, "udp exception", it)
            val errorMessage = it.message ?: it.cause?.message ?: "unknown udp exception"
            addEvent(SessionEvent.networkError(playerId,getCurrentGameTime(),errorMessage))
            reconnect {
                exitWithError(errorMessage, Result.Code.UDP_EXCEPTION)
            }
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