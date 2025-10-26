package com.imsproject.watch.viewmodel

import android.content.Context
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
import com.imsproject.watch.sensors.HeartRateSensorHandler
import com.imsproject.watch.sensors.LocationSensorsHandler
import com.imsproject.watch.utils.ErrorReporter
import com.imsproject.watch.view.contracts.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.java_websocket.exceptions.WebsocketNotConnectedException

private const val TAG = "MainViewModel"

class MainViewModel() : ViewModel() {


    /**
     *  The application flow is the following:
     *  1. [State.DISCONNECTED] - the initial state when the app is opened
     *  2. [State.CONNECTING] - the app is trying to connect to the server for the first time
     *  3. [State.SELECTING_ID] - the user selects an ID to enter with
     *  4. [State.CONNECTING] - trying to enter with the selected ID
     *  5. [State.CONNECTED_NOT_IN_LOBBY] - connected to the server, but not in a lobby
     *  6. [State.CONNECTED_IN_LOBBY] - connected to the server and in a lobby
     *  7. [State.IN_GAME] - the lobby was set up and the game has started
     *  8. [State.AFTER_GAME] - the game has ended and we prepare to upload session events
     *  9. [State.UPLOADING_EVENTS] - uploading session events to the server
     *  10. [State.AFTER_GAME_QUESTIONS] - finished uploading events, now we ask the user post-game questions
     *  11. [State.UPLOADING_ANSWERS] - uploading the answers of the post-game questions to the server
     *  12. [State.EXPERIMENT_QUESTIONS_QR] - if the experiment has ended, we show the QR code to the user
     *  13. [State.THANKS_FOR_PARTICIPATING] - the user has scanned the QR code and we thank them for participating
     *
     *  Note that state 12 and 13 are skipped if the experiment has not ended
     *  and then after state 11 the app returns to state 6.
     */
    enum class State {
     // flow states
        DISCONNECTED,
        CONNECTING,
        SELECTING_ID,
        CONNECTED_NOT_IN_LOBBY,
        CONNECTED_IN_LOBBY,
        IN_GAME,
        UPLOADING_EVENTS,
        AFTER_GAME,
        AFTER_EXPERIMENT,

        // error states
        ALREADY_CONNECTED,
        ERROR,
    }

    private var model = MainModel(viewModelScope)
    val heartRateSensorHandler = HeartRateSensorHandler.instance
    val locationSensorsHandler = LocationSensorsHandler.instance

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

    private var _isWarmup = false
    private var _sessionId = -1
    var temporaryPlayerId = ""

    // ================================================================================ |
    // ============================ PUBLIC METHODS ==================================== |
    // ================================================================================ |

    fun onCreate(context: Context){
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
                    showError("Participant not found")
                    return@launch
                }
                if (playerId != null) {
                    _playerId.value = playerId
                    _loading.value = false
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
        _ready.value = false
        _timeServerStartTime.value = -1
        _gameType.value = null
        _gameDuration.value = null
        _expId.value = result.expId
        val isWarmup = _isWarmup
        setupListeners()
        viewModelScope.launch(Dispatchers.IO) {
            when (result.code) {
                Result.Code.OK -> {
                    setState(State.UPLOADING_EVENTS)
                    if (model.uploadSessionEvents(_sessionId)) {
                        val nextState = if(!isWarmup){
                            State.AFTER_GAME
                        } else if(_lobbyId.value != "") {
                            State.CONNECTED_IN_LOBBY
                        } else {
                            State.CONNECTED_NOT_IN_LOBBY
                        }
                        setState(nextState)
                    } else {
                        fatalError("Failed to upload session events")
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

    fun endExperiment() {
        _expId.value = null
        val nextState = if(_lobbyId.value == ""){
            State.CONNECTED_NOT_IN_LOBBY
        } else {
            State.CONNECTED_IN_LOBBY
        }
        setState(nextState)
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
        _state.value = newState
        Log.d(TAG, "set new state: $newState")
    }

    fun uploadAnswers(vararg QnAs: Pair<String,String>) {
        _loading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            if(model.uploadAfterGameQuestions(_sessionId, *QnAs)){
                _sessionId = -1
                val nextState = if(_expId.value != null){
                    State.AFTER_EXPERIMENT
                } else if(_lobbyId.value != ""){
                    State.CONNECTED_IN_LOBBY
                } else {
                    State.CONNECTED_NOT_IN_LOBBY
                }
                _loading.value = false
                setState(nextState)
            } else {
                fatalError("Failed to upload answers")
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
                    // reset data just in case
                    _gameType.value = null
                    _gameDuration.value = null
                    _ready.value = false
                    _isWarmup = false

                    _lobbyId.value = lobbyId
                    if(_state.value == State.CONNECTED_NOT_IN_LOBBY){
                        setState(State.CONNECTED_IN_LOBBY)
                    }
                }
            }
            GameRequest.Type.LEAVE_LOBBY -> {
                withContext(Dispatchers.Main) {
                    _lobbyId.value = ""
                    if(_state.value == State.CONNECTED_IN_LOBBY){
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

                withContext(Dispatchers.Main){
                    _isWarmup = isWarmup
                    _gameType.value = gameType
                    _gameDuration.value = gameDuration
                    _syncWindowLength.value = syncWindowLength
                    _syncTolerance.value = syncTolerance
                }
            }
            GameRequest.Type.START_GAME -> {
                if(_state.value != State.CONNECTED_IN_LOBBY){
                    Log.e(TAG, "handleGameRequest: START_GAME request received while not in the 'CONNECTED_IN_LOBBY' state")
                    return
                }

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
                    this@MainViewModel._sessionId = _sessionId
                    _timeServerStartTime.value = timeServerStartTime
                    _additionalData.value = request.data?.joinToString(";") ?: ""
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

    fun clearListeners() {
        model.onTcpMessage(null)
        model.onTcpError(null)
        model.onUdpMessage(null)
    }

    fun fatalError(message: String) {
        val oldModel = model
        _playerId.value = ""
        _lobbyId.value = ""
        _gameType.value = null
        _timeServerStartTime.value = -1
        _sessionId = -1
        showError(message)
        viewModelScope.launch(Dispatchers.IO) {
            oldModel.closeAllResources()
            ErrorReporter.report(null, message)
        }
        model = MainModel(viewModelScope)
    }
}