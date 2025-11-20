package com.imsproject.watch.viewmodel

import android.content.Context
import android.os.Vibrator
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imsproject.common.gameserver.GameAction
import com.imsproject.common.gameserver.GameRequest
import com.imsproject.common.gameserver.GameType
import com.imsproject.watch.model.AlreadyConnectedException
import com.imsproject.watch.model.MainModel
import com.imsproject.watch.model.ParticipantNotFoundException
import com.imsproject.watch.model.SessionEventCollectorImpl
import com.imsproject.watch.sensors.HeartRateSensorHandler
import com.imsproject.watch.sensors.LocationSensorsHandler
import com.imsproject.watch.utils.ErrorReporter
import com.imsproject.watch.view.contracts.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

private const val TAG = "MainViewModel"

class MainViewModel() : ViewModel() {

    enum class State {
     // flow states
        DISCONNECTED,
        CONNECTING,
        SELECTING_ID,
        CONNECTED_NOT_IN_LOBBY,
        CONNECTED_IN_LOBBY,
        WELCOME_SCREEN,
        SENSOR_CHECK,
        WAITING_FOR_WELCOME_SCREEN_NEXT,
        COUNTDOWN_TO_GAME,
        LOADING_GAME,
        AFTER_GAME_WAITING,
        COLOR_CONFIRMATION,
        ACTIVITY_DESCRIPTION,
        ACTIVITY_REMINDER,
        GESTURE_PRACTICE,
        WAITING_FOR_GESTURE_PRACTICE_FINISH,
        WAITING_FOR_ACTIVITY_DESCRIPTION_CONFIRMATION,
        WAITING_FOR_RECESS,
        IN_GAME,
        UPLOADING_EVENTS,
        AFTER_GAME_QUESTIONS,
        AFTER_EXPERIMENT,
        THANKS_FOR_PARTICIPATING,
        // error states
        ALREADY_CONNECTED,
        ERROR,
        CONNECTION_LOST

    }

    enum class PlayerColor {
        BLUE,
        GREEN;

        companion object {
            fun fromString(color: String): PlayerColor {
                return when(color.uppercase()){
                    "BLUE" -> BLUE
                    "GREEN" -> GREEN
                    else -> throw IllegalArgumentException("Unknown color: $color")
                }
            }
        }
    }

    private var model = MainModel(viewModelScope)
    val heartRateSensorHandler = HeartRateSensorHandler.instance
    val locationSensorsHandler = LocationSensorsHandler.instance

    lateinit var vibrator: Vibrator

    // ================================================================================ |
    // ================================ STATE FIELDS ================================== |
    // ================================================================================ |

    private val _state = MutableStateFlow(State.DISCONNECTED)
    val state : StateFlow<State> = _state

    private var _loading = MutableStateFlow(false)
    val loading : StateFlow<Boolean> = _loading

    private val _playerId = MutableStateFlow("")
    val playerId : StateFlow<String> = _playerId

    private val _error = MutableStateFlow<String?>(null)
    val error : StateFlow<String?> = _error

    private val _lobbyId = MutableStateFlow("")
    val lobbyId : StateFlow<String> = _lobbyId

    private val _gameType = MutableStateFlow<GameType?>(null)
    val gameType : StateFlow<GameType?> = _gameType

    private val _gameDuration = MutableStateFlow<Int?>(null)
    val gameDuration : StateFlow<Int?> = _gameDuration

    private val _syncWindowLength = MutableStateFlow<Long?>(null)
    val syncWindowLength : StateFlow<Long?> = _syncWindowLength

    private val _syncTolerance = MutableStateFlow<Long?>(null)
    val syncTolerance : StateFlow<Long?> = _syncTolerance

    private val _ready = MutableStateFlow(false)
    val ready : StateFlow<Boolean> = _ready

    private val _timeServerStartTime = MutableStateFlow(-1L)
    val gameStartTime : StateFlow<Long> = _timeServerStartTime

    private val _additionalData = MutableStateFlow("")
    val additionalData : StateFlow<String> = _additionalData

    private val _expId = MutableStateFlow<String?>(null)
    val expId : StateFlow<String?> = _expId

    private val _heartRateUnavailable = MutableStateFlow(false)
    val heartRateUnavailable: StateFlow<Boolean> = _heartRateUnavailable

    private val _myColor = MutableStateFlow(PlayerColor.BLUE)
    val myColor : StateFlow<PlayerColor> = _myColor

    private val _activityIndex = MutableStateFlow(1)
    val activityIndex : StateFlow<Int> = _activityIndex

    private val _isWarmup = MutableStateFlow(false)
    val isWarmup : StateFlow<Boolean> = _isWarmup

    private val _countdownTimer = MutableStateFlow(-1)
    val countdownTimer : StateFlow<Int> = _countdownTimer

    private val _reconnecting = MutableStateFlow(false)
    val reconnecting : StateFlow<Boolean> = _reconnecting

    private val _bytesSent = MutableStateFlow(0L)
    val bytesSent : StateFlow<Long> = _bytesSent

    private val _totalBytes = MutableStateFlow(0L)
    val totalBytes : StateFlow<Long> = _totalBytes

    private var _sessionId = -1
    var temporaryPlayerId = ""

    private var oldGameType: GameType? = null

    var gameTypeChanged = false
        private set

    private var experimentRunning = false
    private var lobbyConfigured = false

    private var connectionRecoveryJob: Job? =  null
    private var prepareNextSessionJob: Job? = null
    private var experimentForceEnded = false

    // ================================================================================ |
    // ============================ PUBLIC METHODS ==================================== |
    // ================================================================================ |

    fun onCreate(context: Context){

        vibrator = context.getSystemService(Vibrator::class.java)

        heartRateSensorHandler.connect(context){ connected, e ->
            if(connected){
                heartRateSensorHandler.init()
            } else {
                Log.e(TAG, "Could not connect to heart rate monitor", e)
                viewModelScope.launch(Dispatchers.Main) {
                    Toast.makeText(context, "Heart rate unavailable", Toast.LENGTH_LONG).show()
                }
                _heartRateUnavailable.value = true
            }
        }
        locationSensorsHandler.init(context)
    }

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
            _loading.value = true
            while (true) {
                val playerId: String?
                try{
                    playerId = model.enter(selectedId,force)
                } catch (_: AlreadyConnectedException){
                    _loading.value = false
                    setState(State.ALREADY_CONNECTED)
                    temporaryPlayerId = selectedId
                    return@launch
                } catch (_: ParticipantNotFoundException){
                    _loading.value = false
                    showError("Participant not found")
                    return@launch
                }
                if (playerId != null) {
                    _playerId.value = playerId
                    _loading.value = false
                    setState(State.CONNECTED_NOT_IN_LOBBY)
                    setupCallbacks() // setup the listeners to start receiving messages
                    return@launch
                } else {
                    Log.d(TAG, "enter: retrying to enter with id $selectedId")
                    delay(100)
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
        _ready.value = false
        _timeServerStartTime.value = -1
        _gameDuration.value = null
        val isWarmup = _isWarmup.value
        setupCallbacks()
        if(experimentForceEnded) {
            SessionEventCollectorImpl.getInstance().clearEvents()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            when (result.code) {
                Result.Code.OK, Result.Code.CONNECTION_LOST -> {
                    if(result.code == Result.Code.CONNECTION_LOST){
                        if(! model.connectionReady){
                            _reconnecting.value = true
                        }
                        while(! model.connectionReady){
                            delay(100)
                            if(_state.value == State.CONNECTION_LOST){
                                return@launch
                            }
                        }
                        _reconnecting.value = false
                    }
                    if(result.uploadEvents){
                        setState(State.UPLOADING_EVENTS)
                    }
                    _bytesSent.value = 0L
                    _totalBytes.value = 0L
                    val uploadSuccess = !result.uploadEvents || model.uploadSessionEvents(_sessionId,30_000, ::updateUploadProgress)
                    if (uploadSuccess) {
                        if(!isWarmup && result.uploadEvents){
                            setState(State.AFTER_GAME_QUESTIONS)
                        } else {
                            prepareNextSession()
                        }
                    } else {
                        Log.e(TAG,"Failed to upload session events")
                        connectionLost()
                    }
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
        Log.e(TAG, string)
        _error.value = if(_error.value != null){
            string + "\n- - - - - - -\n" + _error.value
        } else {
            string
        }
        setState(State.ERROR)
    }

    fun toggleReady() {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d(TAG, "toggling ready")
            model.toggleReady()
            _ready.value = !_ready.value
        }
    }

    fun onDestroy() {
        runBlocking(Dispatchers.Main){
            model.closeAllResources()
            heartRateSensorHandler.disconnect()
        }
    }

    fun setState(newState: State){
        if(experimentForceEnded){
            when(newState){
                State.CONNECTED_IN_LOBBY -> {} // allow going back to lobby
                // reset the flag when going back to disconnected or not in lobby
                State.DISCONNECTED,
                State.CONNECTED_NOT_IN_LOBBY -> {
                    experimentForceEnded = false
                }
                else -> {
                    Log.d(TAG, "setState: experiment force ended, ignoring state change to $newState")
                    return
                }
            }
        }
        _state.value = newState
        Log.d(TAG, "set new state: $newState")
    }

    fun uploadAnswers(QnAs: Map<String,String>) {
        setState(State.UPLOADING_EVENTS)
        _bytesSent.value = 0L
        _totalBytes.value = 0L
        viewModelScope.launch(Dispatchers.IO) {
            if(model.uploadAfterGameQuestions(_sessionId,30_000, QnAs,::updateUploadProgress)){
                _sessionId = -1
                prepareNextSession()
            } else {
                Log.e(TAG,"Failed to upload post-session answers")
                connectionLost()
            }
        }
    }

    fun requestLobbyReconfiguration(){
        viewModelScope.launch(Dispatchers.IO){
            model.requestLobbyReconfiguration()
        }
    }

    // ================================================================================ |
    // ============================ PRIVATE METHODS =================================== |
    // ================================================================================ |

    private suspend fun handleGameRequest(request: GameRequest){
        when (request.type){
            GameRequest.Type.PONG -> {}
            GameRequest.Type.HEARTBEAT -> {}
            GameRequest.Type.EXIT -> {
                Log.d(TAG, "handleGameRequest: EXIT received")
                fatalError(request.message ?: "Connection closed by server")
            }

            // we ignore this here on purpose because it is handled in the GameActivity
            // and this can be sent multiple times after the game ended
            GameRequest.Type.END_GAME -> {}

            GameRequest.Type.JOIN_LOBBY -> {
                val lobbyId = request.lobbyId ?: run {
                    Log.e(TAG, "handleGameRequest: JOIN_LOBBY request missing lobbyId")
                    showError("Failed to join lobby")
                    return
                }

                withContext(Dispatchers.Main) {
                    Log.d(TAG, "handleGameRequest: JOIN_LOBBY received")
                    // reset data just in case
                    _gameDuration.value = null
                    _ready.value = false
                    _isWarmup.value = false
                    _gameType.value = null

                    experimentForceEnded = false

                    _lobbyId.value = lobbyId
                    if(_state.value == State.CONNECTED_NOT_IN_LOBBY){
                        setState(State.CONNECTED_IN_LOBBY)
                    }
                }
            }
            GameRequest.Type.LEAVE_LOBBY -> {
                withContext(Dispatchers.Main) {
                    Log.d(TAG, "handleGameRequest: LEAVE_LOBBY received")
                    _lobbyId.value = ""
                    if(_state.value != State.AFTER_EXPERIMENT){
                        setState(State.CONNECTED_NOT_IN_LOBBY)
                    }
                }
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
                val isWarmup = request.isWarmup ?: run {
                    Log.e(TAG, "handleGameRequest: CONFIGURE_LOBBY request missing isWarmup")
                    showError("Failed to configure lobby")
                    return
                }
                val countdownTimer = request.countdownTimer ?: run {
                    Log.e(TAG, "handleGameRequest: CONFIGURE_LOBBY request missing countdownTimer")
                    showError("Failed to configure lobby")
                    return
                }
                val experimentId = request.experimentId ?: run {
                    Log.e(TAG, "handleGameRequest: CONFIGURE_LOBBY request missing experimentId")
                    showError("Failed to configure lobby")
                    return
                }
                val experimentRunning = request.experimentRunning ?: run {
                    Log.e(TAG, "handleGameRequest: CONFIGURE_LOBBY request missing experimentRunning")
                    showError("Failed to configure lobby")
                    return
                }

                withContext(Dispatchers.Main){
                    Log.d(TAG, """
                        CONFIGURE_LOBBY received:
                            gameType: $gameType
                            gameDuration: $gameDuration
                            syncWindowLength: $syncWindowLength
                            syncTolerance: $syncTolerance
                            isWarmup: $isWarmup
                    """.trimIndent())
                    _isWarmup.value = isWarmup
                    _gameType.value = gameType
                    _gameDuration.value = gameDuration
                    _syncWindowLength.value = syncWindowLength
                    _syncTolerance.value = syncTolerance
                    _countdownTimer.value = countdownTimer
                    _expId.value = experimentId
                    this@MainViewModel.experimentRunning = experimentRunning
                    lobbyConfigured = true

                    // Implementation of gameTypeChanged must be kept in sync with the implementation
                    // in ExperimentOrchestrator in the server
                    if(gameType != GameType.RECESS){
                        Log.d(TAG, "Configure lobby: Game type changed from $oldGameType to $gameType")
                        gameTypeChanged = oldGameType != gameType
                        oldGameType = gameType
                    } else {
                        gameTypeChanged = false
                    }
                    // end of implementation sync
                }
            }
            GameRequest.Type.START_EXPERIMENT -> {
                if(_state.value != State.CONNECTED_IN_LOBBY){
                    Log.e(TAG, "handleGameRequest: START_EXPERIMENT request received while not in the 'CONNECTED_IN_LOBBY' state")
                    return
                }

                val color = request.data?.firstOrNull() ?: run {
                    Log.e(TAG, "handleGameRequest: START_EXPERIMENT request missing color data")
                    showError("Failed to start experiment")
                    return
                }

                withContext(Dispatchers.Main) {
                    Log.d(TAG, "handleGameRequest: START_EXPERIMENT received")
                    experimentRunning = true
                    _myColor.value = PlayerColor.fromString(color)
                    _expId.value = null
                    _activityIndex.value = 1
                    experimentForceEnded = false
                    setState(State.WELCOME_SCREEN)
                }
            }
            GameRequest.Type.END_EXPERIMENT -> {
                val expId = request.experimentId ?: run {
                    Log.e(TAG, "handleGameRequest: END_EXPERIMENT request missing expId data")
                    showError("Failed to end experiment, missing experiment id")
                    return
                }
                val force = request.force ?: run {
                    Log.e(TAG, "handleGameRequest: END_EXPERIMENT request missing force data")
                    showError("Failed to end experiment, missing force flag")
                    return
                }
                val errorMessage = request.message
                withContext(Dispatchers.Main) {
                    Log.d(TAG, "handleGameRequest: END_EXPERIMENT received")
                    if(!experimentRunning){
                        Log.e(TAG, "handleGameRequest: END_EXPERIMENT request received while no experiment is running")
                        return@withContext
                    }

                    if (force) {
                        Log.d(TAG,"Experiment ended forcefully by server")
                        prepareNextSessionJob?.cancelAndJoin()
                        prepareNextSessionJob = null
                        experimentForceEnded = true
                        experimentRunning = false
                        _expId.value = null
                        setState(State.CONNECTED_IN_LOBBY)
                        return@withContext
                    } else if(errorMessage != null) {
                        fatalError("Experiment ended with error: $errorMessage")
                        return@withContext
                    }

                    _expId.value = expId
                    experimentRunning = false
                }
            }
            GameRequest.Type.BOTH_CLIENTS_READY -> {
                withContext(Dispatchers.Main) {
                    Log.d(TAG, "handleGameRequest: BOTH_CLIENTS_READY received")
                    when(_state.value){
                        State.WAITING_FOR_WELCOME_SCREEN_NEXT -> {
                            setState(State.COLOR_CONFIRMATION)
                        }
                        State.WAITING_FOR_GESTURE_PRACTICE_FINISH, State.WAITING_FOR_ACTIVITY_DESCRIPTION_CONFIRMATION -> {
                            setState(State.COUNTDOWN_TO_GAME)
                        }
                        State.WAITING_FOR_RECESS -> {
                            setState(State.LOADING_GAME)
                            toggleReady()
                        }
                        else -> {
                            Log.e(TAG, "handleGameRequest: BOTH_CLIENTS_READY request received in unexpected state: ${_state.value}")
                            return@withContext
                        }
                    }
                }
            }

            GameRequest.Type.START_GAME -> {
                val _sessionId = request.sessionId?.toInt() ?: run{
                    Log.e(TAG, "handleGameRequest: START_GAME request missing sessionId")
                    fatalError("Failed to start game: missing session id")
                    return
                }
                val timeServerStartTime = request.timestamp?.toLong() ?: run{
                    Log.e(TAG, "handleGameRequest: START_GAME request missing start time")
                    fatalError("Failed to start game: missing start time")
                    return
                }

                withContext(Dispatchers.Main) {
                    Log.d(TAG, "handleGameRequest: START_GAME received")
                    if(_state.value != State.LOADING_GAME){
                        Log.e(TAG, "handleGameRequest: START_GAME request received while not in the 'LOADING_GAME' state")
                        return@withContext
                    }
                    this@MainViewModel._sessionId = _sessionId
                    _timeServerStartTime.value = timeServerStartTime
                    _additionalData.value = request.data?.joinToString(";") ?: ""
                    lobbyConfigured = false
                    setState(State.IN_GAME)
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

    private fun prepareNextSession(){
        setState(State.AFTER_GAME_WAITING)
        prepareNextSessionJob = viewModelScope.launch(Dispatchers.Main){
            Log.d(TAG, "prepareNextSession: listener started")
            val waitStartTime = System.currentTimeMillis()
            while(!lobbyConfigured && experimentRunning){
                if(System.currentTimeMillis() - waitStartTime > 3000){
                    requestLobbyReconfiguration()
                }
                delay(100)
            }
            if (! experimentRunning) {
                Log.d(TAG, "prepareNextSession: experiment ended")
                if (_expId.value != null) {
                    Log.d(TAG, "prepareNextSession: moving to AFTER_EXPERIMENT state")
                    setState(State.AFTER_EXPERIMENT)
                } else {
                    if(_lobbyId.value == ""){
                        Log.d(TAG, "prepareNextSession: moving to CONNECTED_NOT_IN_LOBBY state")
                        setState(State.CONNECTED_NOT_IN_LOBBY)
                    } else {
                        Log.d(TAG, "prepareNextSession: moving to CONNECTED_IN_LOBBY state")
                        setState(State.CONNECTED_IN_LOBBY)
                    }
                }
                return@launch
            }

            if(_gameType.value == GameType.RECESS){
                setState(State.WAITING_FOR_RECESS)
                toggleReady()
                return@launch
            }

            if(gameTypeChanged){
                Log.d(TAG, "prepareNextSession: game type changed")
                _activityIndex.value += 1
                setState(State.ACTIVITY_DESCRIPTION)
            } else {
                Log.d(TAG, "prepareNextSession: game type did not change")
                if(_isWarmup.value){
                    setState(State.ACTIVITY_REMINDER)
                } else {
                    setState(State.COUNTDOWN_TO_GAME)
                }
            }
        }
    }

    private fun handleGameAction(action: GameAction){
        when(action.type){
            GameAction.Type.USER_INPUT -> {}
            else -> {
                Log.e(TAG, "handleGameAction: Unexpected action type: ${action.type}")
                val errorMsg = "Unexpected request type received\n" +
                        "request type: ${action.type}\n"+
                        "request content:\n$action"
                showError(errorMsg)
            }
        }
    }

    private suspend fun updateUploadProgress(bytesSent: Long, totalBytes: Long){
        withContext(Dispatchers.Main){
            if(_bytesSent.value >= bytesSent) return@withContext // avoid going backwards

            Log.d(TAG, "upload progress: $bytesSent / $totalBytes (${(bytesSent / totalBytes.toFloat()) * 100f}%)")
            _bytesSent.value = bytesSent
            _totalBytes.value = totalBytes
        }
    }

    private fun setupCallbacks() {
        //TODO: add callbacks for onRecoveringConnection and onConnectionRecovered
        // and update the existing ones

        // TCP
        model.onTcpMessage { handleGameRequest(it) }
        model.onTcpException {
            Log.e(TAG, "tcp exception", it)
            showError(it.message ?: it.cause?.message ?: "unknown tcp exception")
            ErrorReporter.report(it)
        }
        model.onTcpError {
            Log.e(TAG, "tcp error", it)
            showError(it.message ?: it.cause?.message ?: "unknown tcp error")
            ErrorReporter.report(it)
        }

        // UDP
        model.onUdpMessage { handleGameAction(it) }
        model.onUdpException {
            Log.e(TAG, "udp exception", it)
            showError(it.message ?: it.cause?.message ?: "unknown udp exception")
            ErrorReporter.report(it)
        }

        model.onRecoveringConnection {
            withContext(Dispatchers.Main){
                if(connectionRecoveryJob == null){
                    connectionRecoveryJob = viewModelScope.launch(Dispatchers.IO) {
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
                }
            }
        }
    }

    fun clearCallbacks(){
        model.onTcpMessage(null)
        model.onTcpError(null)
        model.onTcpException(null)
        model.onUdpMessage(null)
        model.onUdpException(null)
        model.onRecoveringConnection(null)
        model.onConnectionRecovered(null)
    }

    fun connectionLost(){
        _reconnecting.value = false
        fatalError("Connection lost", false)
        setState(State.CONNECTION_LOST)
    }

    fun fatalError(message: String, showError: Boolean = true) {
        val oldModel = model
        _playerId.value = ""
        _lobbyId.value = ""
        _gameType.value = null
        _timeServerStartTime.value = -1
        _sessionId = -1
        if(showError){
            showError(message)
        }
        viewModelScope.launch(Dispatchers.IO) {
            oldModel.closeAllResources()
            ErrorReporter.report(null, message)
        }
        model = MainModel(viewModelScope)
    }
}