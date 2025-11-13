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
import com.imsproject.watch.model.SERVER_IP
import com.imsproject.watch.model.SERVER_UDP_PORT
import com.imsproject.watch.model.SessionEventCollector
import com.imsproject.watch.model.SessionEventCollectorImpl
import com.imsproject.watch.sensors.HeartRateSensorHandler
import com.imsproject.watch.sensors.LocationSensorsHandler
import com.imsproject.watch.utils.ErrorReporter
import com.imsproject.watch.utils.LatencyTracker
import com.imsproject.watch.utils.PacketTracker
import com.imsproject.watch.utils.WavPlayer
import com.imsproject.watch.view.contracts.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


abstract class GameViewModel(
    gameType: GameType
) : ViewModel(), SessionEventCollector by SessionEventCollectorImpl.getInstance() {

    enum class State {
        LOADING,
        PLAYING,
        ERROR,
        TERMINATED
    }

    private val TAG = "$_TAG-${gameType.prettyName()}"
    val model = if(ACTIVITY_DEBUG_MODE) MainModel(viewModelScope) else MainModel.instance
    val playerId : String = model.playerId ?: run {
        Log.e(TAG, "init: missing player ID")
        "unknown player ID"
    }

    lateinit var vibrator: Vibrator
        private set
    protected val packetTracker = PacketTracker()
    private lateinit var latencyTracker : LatencyTracker
    private val heartRateSensorHandler: HeartRateSensorHandler = HeartRateSensorHandler.instance
    private val locationSensorsHandler: LocationSensorsHandler = LocationSensorsHandler.instance
    lateinit var wavPlayer: WavPlayer
        protected set

    var screenDensity: Float = -1f

    // ================================================================================ |
    // ================================ STATE FIELDS ================================== |
    // ================================================================================ |

    private val _state = MutableStateFlow(State.LOADING)
    val state : StateFlow<State> = _state

    private val _error = MutableStateFlow<String?>(null)
    val error : StateFlow<String?> = _error

    private val _resultCode = MutableStateFlow(Result.Code.OK)
    val resultCode : StateFlow<Result.Code> = _resultCode

    private val _reconnecting = MutableStateFlow(false)
    val reconnecting : StateFlow<Boolean> = _reconnecting

    private val _progress = MutableStateFlow(0)
    val progress : StateFlow<Int> = _progress

    private var gameDuration = 1000000000

    private var timeServerDelta = 0L
    protected var myStartTime = 0L
    private var connectionRecoveryJob: Job? = null

    // ================================================================================ |
    // ============================ PUBLIC METHODS ==================================== |
    // ================================================================================ |

    open fun onCreate(intent: Intent, context: Context){

        vibrator = context.getSystemService(Vibrator::class.java)
        wavPlayer = WavPlayer(context, viewModelScope)

        if(ACTIVITY_DEBUG_MODE){
            myStartTime = System.currentTimeMillis()
            setState(State.PLAYING)
            return
        }

        // set up everything required for the session
        viewModelScope.launch(Dispatchers.Default) {
            clearEvents() // clear any events from previous sessions

            // clock setup
            val timeServerStartTime = intent.getLongExtra("$PACKAGE_PREFIX.timeServerStartTime",-1).also {
                if(it <= 0){
                    Log.e(TAG, "onCreate: missing time server start time")
                    exitWithError("Missing time server start time", Result.Code.BAD_REQUEST)
                    return@launch
                }
            }
            timeServerDelta = model.calculateTimeServerDelta(::updateSuccessfulTimeRequests)
            myStartTime = timeServerStartTime + timeServerDelta

            val gameDuration = intent.getIntExtra("$PACKAGE_PREFIX.gameDuration", -1).also {
                if (it <= 0) {
                    exitWithError("Missing or invalid game duration", Result.Code.BAD_REQUEST)
                    return@launch
                }
            }
            this@GameViewModel.gameDuration = gameDuration

            // log clock metadata
            val timestamp = getCurrentGameTime()
            addEvent(SessionEvent.serverStartTime(playerId, timestamp,timeServerStartTime.toString()))
            addEvent(SessionEvent.timeServerDelta(playerId,timestamp,timeServerDelta.toString()))
            addEvent(SessionEvent.clientStartTime(playerId,timestamp,myStartTime.toString()))

            setupCallbacks() // start listening for messages

            // packet tracker setup
            packetTracker.onOutOfOrderPacket = {
                addEvent(SessionEvent.packetOutOfOrder(playerId,getCurrentGameTime()))
            }

            // latency tracker setup
            latencyTracker = LatencyTracker(
                viewModelScope,
                GameAction.ping,
                SERVER_IP,
                SERVER_UDP_PORT
            )
            latencyTracker.onReceive = {
                addEvent(SessionEvent.latency(playerId,getCurrentGameTime(),it.toString()))
            }
            latencyTracker.onTimeout = {
                addEvent(SessionEvent.timedOut(playerId,getCurrentGameTime(),it.toString()))
            }

            // start collecting latency statistics
            viewModelScope.launch(Dispatchers.Default) {
                latencyTracker.start()
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

            // start tracking sensors
            locationSensorsHandler.startTracking(this@GameViewModel)
            heartRateSensorHandler.startTracking(this@GameViewModel)

            // failsafe end of session job - in case END_GAME request is lost due to network issues
            viewModelScope.launch {
                delay(gameDuration*1000 + 5000L) // add 5 seconds buffer
                Log.d(TAG, "Failsafe end of session triggered")
                if(! model.connectionReady){
                    Log.d(TAG, "failsafe end of session: still reconnecting, exiting with error")
                    exitWithError("Failsafe end of session: still reconnecting", Result.Code.CONNECTION_LOST)
                } else {
                    Log.d(TAG, "Failsafe end of session: exiting with ok")
                    exitOk()
                }
            }
        }
    }

    fun exitWithError(errorMessage: String, code: Result.Code) {
        Log.e(TAG, "exitWithError: game ended with code: $code and error: $errorMessage")
        addEvent(SessionEvent.sessionEnded(playerId,getCurrentGameTime(),errorMessage))
        _error.value = errorMessage
        _resultCode.value = code
        onExit()
    }

    fun getCurrentGameTime(): Long {
        return System.currentTimeMillis() - myStartTime
    }

    fun showError(string: String) {
        Log.e(TAG, string)
        _error.value = if(_error.value != null){
            string + "\n- - - - - - -\n" + _error.value
        } else {
            string
        }
        setState(State.ERROR)
    }

    fun clearError() {
        if(_state.value == State.ERROR){
            Log.d(TAG, "clearError: clearing error")
            _error.value = null
            setState(State.PLAYING)
        }
    }

    fun onWavPlayerException(){
        wavPlayer.releaseAll()
        setupWavPlayer()
    }

    // ================================================================================ |
    // ============================ PROTECTED METHODS ================================= |
    // ================================================================================ |

    protected open suspend fun handleGameAction(action: GameAction) {
        when (action.type) {
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
            GameRequest.Type.SESSION_SETUP_COMPLETE -> startGame()
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
        onExit()
    }

    protected open fun onExit(){
        clearCallbacks() // clear the listeners to prevent any further messages from being processed.
        if(locationSensorsHandler.tracking){
            locationSensorsHandler.stopTracking()
        }
        if(heartRateSensorHandler.tracking){
            heartRateSensorHandler.stopTracking()
        }
        if(::wavPlayer.isInitialized){
            wavPlayer.pauseAll()
            wavPlayer.releaseAll()
        }
        setState(State.TERMINATED)
    }

    protected open fun setupWavPlayer(){}

    protected open fun startGame() {
        addEvent(SessionEvent.sessionStarted(playerId,getCurrentGameTime()))
        Log.d(TAG, "startGame(): session started")
        setState(State.PLAYING)
    }

    // ================================================================================ |
    // ============================ PRIVATE METHODS =================================== |
    // ================================================================================ |

    private fun updateSuccessfulTimeRequests(count: Int){
        _progress.value = count
    }

    private fun setupCallbacks() {
        //TODO: add callbacks for onRecoveringConnection and onConnectionRecovered
        // and update the existing ones

        // TCP
        model.onTcpMessage { handleGameRequest(it) }
        model.onTcpException {
            Log.e(TAG, "tcp exception", it)
            val errorMessage = it.message ?: it.cause?.message ?: "unknown tcp exception"
            addEvent(SessionEvent.networkError(playerId,getCurrentGameTime(),errorMessage))
            ErrorReporter.report(it)
            exitWithError(errorMessage, Result.Code.TCP_EXCEPTION)
        }
        model.onTcpError {
            Log.e(TAG, "tcp error", it)
            val errorMessage = it.message ?: it.cause?.message ?: "unknown tcp error"
            addEvent(SessionEvent.networkError(playerId,getCurrentGameTime(),errorMessage))
            ErrorReporter.report(it)
            exitWithError(errorMessage, Result.Code.TCP_ERROR)
        }

        // UDP
        model.onUdpMessage { handleGameAction(it) }
        model.onUdpException {
            Log.e(TAG, "udp exception", it)
            val errorMessage = it.message ?: it.cause?.message ?: "unknown udp exception"
            addEvent(SessionEvent.networkError(playerId,getCurrentGameTime(),errorMessage))
            ErrorReporter.report(it)
            exitWithError(errorMessage, Result.Code.UDP_EXCEPTION)
        }

        model.onRecoveringConnection {
            withContext(Dispatchers.Main){
                if(connectionRecoveryJob == null){
                    connectionRecoveryJob = viewModelScope.launch(Dispatchers.IO) {
                        addEvent(SessionEvent.networkError(playerId,getCurrentGameTime(),"Connection lost"))
                        delay(5000) // wait for 5 seconds before showing reconnecting overlay
                        _reconnecting.value = true
                    }
                }
            }
        }

        model.onConnectionRecovered {
            withContext(Dispatchers.Main){
                val job = connectionRecoveryJob
                withContext(Dispatchers.IO){
                    job?.cancel()
                    connectionRecoveryJob = null
                    _reconnecting.value = false
                    setupCallbacks()
                    addEvent(SessionEvent.reconnected(playerId,getCurrentGameTime()))
                }
            }
        }
    }

    private fun setState(newState: State){
        _state.value = newState
        Log.d(TAG, "set new state: $newState")
    }

    private fun clearCallbacks(){
        model.onTcpMessage(null)
        model.onTcpError(null)
        model.onTcpException(null)
        model.onUdpMessage(null)
        model.onUdpException(null)
        model.onRecoveringConnection(null)
        model.onConnectionRecovered(null)
    }

    companion object {
        private const val _TAG = "GameViewModel"
    }
}