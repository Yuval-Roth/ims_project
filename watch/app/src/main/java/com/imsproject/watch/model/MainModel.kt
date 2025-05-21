package com.imsproject.watch.model

import android.util.Log
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import com.imsproject.common.etc.TimeRequest
import com.imsproject.common.gameserver.GameAction
import com.imsproject.common.gameserver.GameRequest
import com.imsproject.common.networking.UdpClient
import com.imsproject.common.networking.WebSocketClient
import com.imsproject.common.utils.Response
import com.imsproject.common.utils.fromJson
import com.imsproject.common.utils.toJson
import com.imsproject.watch.utils.RestApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull
import org.java_websocket.exceptions.WebsocketNotConnectedException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URI
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import kotlin.math.roundToLong

// set these values to run the app locally
private const val RUNNING_LOCAL : Boolean = false
private const val RUNNING_ON_EMULATOR : Boolean = false
private const val COMPUTER_NETWORK_IP = "192.168.0.102"

// ========== Constants ===========|
private const val TIMEOUT_MS = 2000L
private const val REMOTE_IP = "ims-project.cs.bgu.ac.il"
private val LOCAL_IP = if(RUNNING_ON_EMULATOR) "10.0.2.2" else COMPUTER_NETWORK_IP
val SERVER_IP = if (RUNNING_LOCAL) LOCAL_IP else REMOTE_IP
val WS_SCHEME = if (RUNNING_LOCAL) "ws" else "wss"
val REST_SCHEME = if (RUNNING_LOCAL) "http" else "https"
val SERVER_HTTP_PORT = if (RUNNING_LOCAL) 8080 else 8640
val SERVER_ERROR_REPORTS_PORT = if (RUNNING_LOCAL) 8085 else 8645
const val SERVER_UDP_PORT = 8641
private const val TIME_SERVER_PORT = 8642
// ================================|

class AlreadyConnectedException : Exception("Already connected",null,true,false)
class ParticipantNotFoundException: Exception("Participant id not found",null,true,false)

class MainModel (private val scope : CoroutineScope) {

    private class CallbackNotSetException : Exception("Listener not set",null,true,false)

    private lateinit var ws: WebSocketClient
    private lateinit var udp : UdpClient
    private var clientsClosed = true

    init{
        instance = this
        try {
            Class.forName("dalvik.system.CloseGuard")
                .getMethod("setEnabled", Boolean::class.javaPrimitiveType)
                .invoke(null, true)
        } catch (e: ReflectiveOperationException) {
            throw RuntimeException(e)
        }
    }

    var playerId : String? = null
        private set
    var connected = false
        private set

    private val cachedException = CallbackNotSetException()
    private val defaultCallback  = { throw cachedException }
    private var tcpOnMessageCallback :  suspend (GameRequest) -> Unit = { defaultCallback() }
    private var tcpOnExceptionCallback : suspend (Exception) -> Unit = { defaultCallback() }
    private var udpOnMessageCallback : suspend (GameAction) -> Unit = { defaultCallback() }
    private var udpOnExceptionCallback : suspend (Exception) -> Unit = { defaultCallback() }
    private var tcpOnErrorCallback : suspend (Exception) -> Unit = { defaultCallback() }

    private var tcpMessageListener : Job? = null
    private var udpMessageListener : Job? = null
    private var wsHeartBeatListener : Job? = null
    private var udpHeartBeatListener : Job? = null

    private val wsChannel = Channel<GameRequest>()
    private val udpChannel = Channel<GameAction>()
    private val wsHeartBeatChannel = Channel<GameRequest>(1, BufferOverflow.DROP_OLDEST)
    private val udpHeartBeatChannel = Channel<GameAction>(1, BufferOverflow.DROP_OLDEST)

    private val connectionRecoveryLock = Mutex(false)

    // ======================================================================= |
    // ======================== Public Methods =============================== |
    // ======================================================================= |

    fun connectToServer(timeout: Long = 10) : Boolean {
        Log.d(TAG, "connectToServer: Connecting to server")

        if(this::ws.isInitialized){
            if(! ws.isOpen || ws.isClosing || ws.isClosed){
                clientsClosed = true
            }
        }

        if (clientsClosed) {
            val (ws, udp) = getNewClients()
            this.ws = ws
            this.udp = udp
            clientsClosed = false
        }

        if(!ws.isOpen){
            if(!ws.connectBlocking(timeout, TimeUnit.SECONDS)){
                Log.e(TAG, "connectToServer: WebSocket connection timeout")
                return false // timeout
            }
            startListeners()
            startHeartBeat()
        }
        return true
    }

    /**
     * Establishes a connection to the game server via both WebSocket and UDP protocols.
     * @return the player ID if the connection is successful, or null if any step fails.
     * @throws AlreadyConnectedException if the client is already connected.
     * @throws ParticipantNotFoundException if the selected ID is not found on the server.
     */
    @Throws(AlreadyConnectedException::class, ParticipantNotFoundException::class)
    suspend fun enter(selectedId: String, force: Boolean = false) : String? {
        Log.d(TAG, "enter: Connecting to server with id $selectedId")

        if(clientsClosed) throw IllegalStateException("Clients are closed")

        // start setup
        val enterType = if(force) GameRequest.Type.FORCE_ENTER else GameRequest.Type.ENTER

        val udpEnterCode = wsSetup(enterType, selectedId) ?: return null
        if(! udpSetup(udpEnterCode)) return null

        // connection established
        this.playerId = selectedId
        connected = true

        Log.d(TAG, "enter: Connection established")
        return playerId
    }

    suspend fun reconnect() : Boolean {
        Log.d(TAG, "reconnect: Reconnecting to server")
        val playerId = this.playerId ?: run {
            Log.e(TAG, "reconnect: Missing player id")
            return false
        }

        if(clientsClosed) throw IllegalStateException("Clients are closed")

        // start setup
        val udpEnterCode = wsSetup(GameRequest.Type.RECONNECT, playerId) ?: return false
        if(! udpSetup(udpEnterCode)) return false

        // connection established
        connected = true

        Log.d(TAG, "reconnect: Connection established")
        return true
    }

    fun exit() {
        connected = false
        sendTcp(GameRequest.builder(GameRequest.Type.EXIT).build().toJson())
    }

    /**
     * Sets or removes the callback for UDP message and exception events.
     * By default, the exception callback is set to an empty function.
     *
     * @param callback If set to **null**, the callback is removed and no messages will be received.
     */
    fun onUdpMessage(callback: (suspend (GameAction) -> Unit)?,  onException: suspend (Exception) -> Unit = {}) {
        if(callback == null){
            udpOnMessageCallback = {defaultCallback()}
            udpOnExceptionCallback = {}
            Log.d(TAG, "onUdpMessage: callback removed")
        } else {
            udpOnMessageCallback = callback
            udpOnExceptionCallback = onException
            Log.d(TAG, "onUdpMessage: callback set")
        }
    }

    /**
     * Sets or removes the callback for TCP message and exception events.
     * By default, the exception callback is set to an empty function.
     *
     * @param callback If set to **null**, the callback is removed and no messages will be received.
     */
    fun onTcpMessage(callback:  (suspend (GameRequest) -> Unit)?, onException: suspend (Exception) -> Unit = {}) {

        if(callback == null){
            tcpOnMessageCallback = {defaultCallback()}
            tcpOnExceptionCallback = {}
            Log.d(TAG, "onTcpMessage: callback removed")
        } else {
            tcpOnMessageCallback = callback
            tcpOnExceptionCallback = onException
            Log.d(TAG, "onTcpMessage: callback set")
        }
    }

    /**
     * Sets or removes the callback for TCP error events.
     *
     * @param callback If set to **null**, the callback is removed and no messages will be received.
     */
    fun onTcpError(callback: (suspend (Exception) -> Unit)?) {

        if(callback == null){
            tcpOnErrorCallback = {defaultCallback()}
            Log.d(TAG, "onTcpError: callback removed")
        } else {
            tcpOnErrorCallback = callback
            Log.d(TAG, "onTcpError: callback set")
        }
    }

    fun toggleReady() {
        val request = GameRequest.builder(GameRequest.Type.TOGGLE_READY).build().toJson()
        sendTcp(request)
    }

    fun sendUserInput(timestamp: Long, sequenceNumber: Long, data: String? = null) {
        val request = GameAction.builder(GameAction.Type.USER_INPUT)
            .actor(playerId)
            .apply{ data?.let { data(it) } }
            .timestamp(timestamp.toString())
            .sequenceNumber(sequenceNumber)
            .build().toString()
        sendUdp(request)
    }

    fun calculateTimeServerDelta(): Long {
        val request = TimeRequest.request(TimeRequest.Type.CURRENT_TIME_MILLIS).toJson()
        val timeServerUdp = UdpClient().apply{ init(); setTimeout(100) }
        var count = 0
        val data = mutableListOf<Long>()
        while(count < 100){
            try {
                val currentLocalTime = System.currentTimeMillis()
                timeServerUdp.send(request, SERVER_IP, TIME_SERVER_PORT)
                val response = timeServerUdp.receive()
                val timeDelta = System.currentTimeMillis() - currentLocalTime
                val timeResponse = fromJson<TimeRequest>(response)
                val currentServerTime = timeResponse.time!! - timeDelta / 2 // approximation
                data.add(currentLocalTime-currentServerTime)
                count++
            } catch (e: SocketTimeoutException) {
                Log.e(TAG, "Time request timeout", e)
            } catch (e: JsonParseException) {
                Log.e(TAG, "Failed to parse time response", e)
            } catch (e: IOException) {
                Log.e(TAG, "Failed to fetch time", e)
            }
        }
        timeServerUdp.close()
        return data.average().roundToLong()
    }

    suspend fun uploadSessionEvents(sessionId: Int): Boolean {
        Log.d(TAG, "Uploading session events")
        val eventCollector = SessionEventCollectorImpl.getInstance()

        // split events into chunks of 5000
        val eventLists = mutableListOf<MutableList<String>>()
        for(event in eventCollector.getAllEvents()){
            val compressedEvent = event.toCompressedJson()
            if(eventLists.isEmpty() || eventLists.last().size >= 5000){
                eventLists.add(mutableListOf(event.toCompressedJson()))
            } else {
                eventLists.last().add(compressedEvent)
            }
        }

        val deferreds = mutableListOf<Deferred<Boolean>>()

        eventLists.forEachIndexed { part, eventList ->
            deferreds.add(scope.async(Dispatchers.IO) {
                val body = object {
                    val sessionId = sessionId
                    val events = eventList
                }.toJson()

                var tries = 0
                while(tries < 5){
                    val returned = RestApiClient()
                        .withUri("$REST_SCHEME://$SERVER_IP:$SERVER_HTTP_PORT/data")
                        .withBody(body)
                        .withPost()
                        .send()

                    val response: Response
                    try{
                        response = fromJson<Response>(returned)
                    } catch(e: JsonSyntaxException){
                        Log.e(TAG, "uploadSessionEvents: Part ${part+1} failed to parse response: $returned", e)
                        throw RuntimeException("uploadSessionEvents: Part ${part+1} failed to parse response: $returned",e)
                    }

                    if(response.success){
                        Log.d(TAG, "uploadSessionEvents: Part ${part+1} success")
                        return@async true
                    } else {
                        Log.e(TAG, "uploadSessionEvents: Part ${part+1} Failed to upload events\n${response.message}")
                        tries++
                    }
                }
                return@async false
            })
        }

        // check if all api calls succeeded
        val success = deferreds.all { it.await() }

        eventCollector.clearEvents()
        return success
    }

    fun uploadAfterGameQuestions(sessionId: Int, vararg QnAs: Pair<String,String>): Boolean {
        Log.d(TAG, "Uploading after game questions")
        val body = object {
            val sessionId = sessionId
            val qnas = QnAs.map {
                object {
                    val question = it.first
                    val answer = it.second
                }
            }.toList()
        }.toJson()
        var tries = 0
        while(tries < 5){
            val returned = RestApiClient()
                .withUri("$REST_SCHEME://$SERVER_IP:$SERVER_HTTP_PORT/data/session/insert/feedback")
                .withBody(body)
                .withPost()
                .send()

            val response: Response
            try{
                response = fromJson<Response>(returned)
            } catch(e: JsonSyntaxException){
                Log.e(TAG, "uploadAfterGameQuestions: failed to parse response: $returned", e)
                throw RuntimeException("uploadAfterGameQuestions: failed to parse response: $returned",e)
            }

            if(response.success){
                Log.d(TAG, "uploadAfterGameQuestions: success")
                return true
            } else {
                Log.e(TAG, "uploadAfterGameQuestions: Failed to upload events\n${response.message}")
                tries++
            }
        }
        return false
    }

    suspend fun closeAllResources(){
        Log.d(TAG, "Closing all resources")
        try{
            connected = false
            stopListeners()
            stopHeartBeat()
            ws.close()
            udp.close()
            clientsClosed = true
            Log.d(TAG, "All resources closed")
        } catch(e: Exception){
            Log.e(TAG, "Failed to close resources", e)
        }
    }

    // ======================================================================= |
    // ======================== Private Methods ============================== |
    // ======================================================================= |

    private suspend fun handleGameRequest(request: GameRequest){
        when(request.type){
            GameRequest.Type.HEARTBEAT -> wsHeartBeatChannel.send(request)
            GameRequest.Type.PING -> {}
            GameRequest.Type.PONG -> {}

            GameRequest.Type.ENTER,
            GameRequest.Type.FORCE_ENTER,
            GameRequest.Type.ALREADY_CONNECTED,
            GameRequest.Type.PARTICIPANT_NOT_FOUND,
            GameRequest.Type.RECONNECT -> wsChannel.send(request)

            else -> executeCallback { tcpOnMessageCallback(request) }
        }
    }

    private suspend fun handleGameAction(action: GameAction){
        when(action.type){
            GameAction.Type.USER_INPUT -> executeCallback { udpOnMessageCallback(action) }

            GameAction.Type.HEARTBEAT -> udpHeartBeatChannel.send(action)
            GameAction.Type.PING -> {}
            GameAction.Type.PONG -> {}

            GameAction.Type.ENTER -> udpChannel.send(action)

            else -> executeCallback { udpOnMessageCallback(action) }
        }
    }

    private fun getNewClients(): Pair<WebSocketClient,UdpClient> {
        val ws = WebSocketClient(URI("$WS_SCHEME://$SERVER_IP:$SERVER_HTTP_PORT/ws"))
        ws.connectionLostTimeout = -1
        val udp = UdpClient()
        udp.remoteAddress = SERVER_IP
        udp.remotePort = SERVER_UDP_PORT
        udp.init()
        return ws to udp
    }

    @Throws(AlreadyConnectedException::class, ParticipantNotFoundException::class)
    private suspend fun wsSetup (
        connectType: GameRequest.Type,
        id: String
    ): String? {

        // Send enter request and wait for response
        val enterRequest = GameRequest.builder(connectType)
            .playerId(id)
            .build().toJson()
        sendTcp(enterRequest)

        val enterResponse = withTimeoutOrNull(TIMEOUT_MS){
            wsChannel.receive()
        } ?: run {
            Log.e(TAG, "wsSetup: WebSocket $connectType request timeout")
            return null // timeout
        }

        if(enterResponse.type == GameRequest.Type.ALREADY_CONNECTED){
            Log.d(TAG, "wsSetup: Already connected")
            throw AlreadyConnectedException()
        }

        if(enterResponse.type == GameRequest.Type.PARTICIPANT_NOT_FOUND){
            Log.d(TAG, "wsSetup: Participant not found")
            throw ParticipantNotFoundException()
        }

        if(enterResponse.type != connectType){
            Log.e(TAG, "wsSetup: Invalid response type ${enterResponse.type}")
            return null // invalid response
        }

        val udpEnterCode = enterResponse.data?.get(0) ?: run {
            Log.e(TAG, "wsSetup: Missing UDP enter code")
            return null // invalid response
        }

       return udpEnterCode
    }

    private suspend fun udpSetup(
        udpEnterCode: String
    ): Boolean {

        // send ENTER request with the code
        sendUdp(GameAction.builder(GameAction.Type.ENTER)
                .data(udpEnterCode)
                .build().toString())

        // === wait for confirmation === |
        val enterResponse = withTimeoutOrNull(TIMEOUT_MS){
            udpChannel.receive()
        } ?: run {
            Log.e(TAG, "udpSetup: UDP enter timeout")
            return false // timeout
        }

        if(enterResponse.type != GameAction.Type.ENTER){
            Log.e(TAG, "udpSetup: Invalid response type ${enterResponse.type}")
        }
        // === end of confirmation === |

        return true
    }

    private suspend fun executeCallback(action: suspend () -> Unit){
        while(true){
            try{
                action()
                break
            } catch(_: CallbackNotSetException){
                delay(100)
            }
        }
    }

    private fun startHeartBeat(){
        Log.d(TAG, "Starting heartbeat")

        // TCP
        wsHeartBeatListener = scope.launch(Dispatchers.IO){
            var failedTries = 0
            while(isActive){
                sendTcp(GameRequest.heartbeat)
                val received = withTimeoutOrNull(1000){
                    wsHeartBeatChannel.receive()
                }
                if (received == null){
                    failedTries++
                    if(failedTries > 3){
                        attemptConnectionRecovery {
                            executeCallback { tcpOnExceptionCallback(WebsocketNotConnectedException()) }
                        }
                    }
                    continue
                }
                failedTries = 0
                delay(5000)
            }
        }

        // UDP
        udpHeartBeatListener = scope.launch(Dispatchers.IO){
            while(isActive){
                sendUdp(GameAction.heartbeat)
                val received = withTimeoutOrNull(1000){
                    udpHeartBeatChannel.receive()
                }
                if (received == null){
                    continue
                }
                delay(5000)
            }
        }
    }

    private suspend fun stopHeartBeat(){
        Log.d(TAG, "Stopping heartbeat")
        wsHeartBeatListener?.cancel()
        udpHeartBeatListener?.cancel()
        wsHeartBeatListener?.join()
        udpHeartBeatListener?.join()
        wsHeartBeatListener = null
        udpHeartBeatListener = null
    }

    private fun startListeners() {
        Log.d(TAG, "Starting listeners")

        // UDP
        udpMessageListener = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val message = udp.receive()
                    val action = GameAction.fromString(message)
                    handleGameAction(action)
                } catch (e: IOException) {
                    Log.e(TAG, "Failed to receive UDP message", e)
                    attemptConnectionRecovery {
                        executeCallback { udpOnExceptionCallback(e) }
                    }
                    return@launch
                } catch (e: JsonParseException) {
                    Log.e(TAG, "Failed to parse UDP message", e)
                    executeCallback { udpOnExceptionCallback(e) }
                    return@launch
                }  catch(_: InterruptedException){
                    Log.d(TAG, "udp listener interrupted")
                    return@launch
                }
            }
        }

        tcpMessageListener = scope.launch(Dispatchers.IO){
            while(isActive){
                try{
                    val message = ws.nextMessageBlocking()
                    val request = fromJson<GameRequest>(message)
                    handleGameRequest(request)
                } catch(e: JsonParseException){
                    Log.e(TAG, "Failed to parse TCP message", e)
                    executeCallback { tcpOnExceptionCallback(e) }
                    return@launch
                } catch(_: InterruptedException){
                    Log.d(TAG, "WebSocket listener interrupted")
                    return@launch
                }
            }
        }

        ws.onErrorListener = {
            scope.launch(Dispatchers.IO){
                executeCallback { tcpOnErrorCallback(it ?: Exception("Unknown error"))}
            }
        }
    }

    private suspend fun stopListeners(){
        Log.d(TAG, "Stopping listeners")
        udpMessageListener?.cancel()
        tcpMessageListener?.cancel()
        ws.onErrorListener = {}
        ws.interrupt()
        udp.interrupt()
        tcpMessageListener?.join()
        udpMessageListener?.join()
        tcpMessageListener = null
        udpMessageListener = null
    }

    private fun sendTcp(message: String){
        try{
            ws.send(message)
        } catch(e: WebsocketNotConnectedException) {
            Log.e(TAG, "sendTcp: sending message failed", e)
            attemptConnectionRecovery {
                executeCallback { tcpOnExceptionCallback(e) }
            }
        }
    }

    private fun sendUdp(message: String){
        try{
            udp.send(message)
        } catch (e : Exception){
            Log.e(TAG, "Failed to send UDP message", e)
            attemptConnectionRecovery {
                executeCallback { udpOnExceptionCallback(e) }
            }
        }
    }

    private fun attemptConnectionRecovery(onFailure: suspend () -> Unit) {

        if(connectionRecoveryLock.tryLock()){
            scope.launch(Dispatchers.IO) {
                Log.d(TAG, "Attempting connection recovery")
                val success = if(!connected){
                    ws.reset()
                    ws.connectBlocking(TIMEOUT_MS, TimeUnit.MILLISECONDS)
                } else {
                    closeAllResources()
                    connectToServer(2) && reconnect()
                }
                connectionRecoveryLock.unlock()
                if(success){
                    Log.d(TAG, "Connection recovery successful")
                } else {
                    Log.e(TAG, "Connection recovery failed")
                    onFailure()
                }
            }
        }
    }

    companion object {
        private const val TAG = "MainModel"
        lateinit var instance : MainModel
            private set
    }

    private fun org.java_websocket.client.WebSocketClient.reset() {
        this::class.java.superclass.getDeclaredMethod("reset").apply{
            isAccessible = true
            invoke(this@reset)
        }
    }
}