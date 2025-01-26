package com.imsproject.watch.model

import android.util.Log
import com.google.gson.JsonParseException
import com.imsproject.common.etc.TimeRequest
import com.imsproject.common.gameserver.GameAction
import com.imsproject.common.gameserver.GameRequest
import com.imsproject.common.networking.UdpClient
import com.imsproject.watch.utils.LatencyTracker
import com.imsproject.common.networking.WebSocketClient
import com.imsproject.common.utils.Response
import com.imsproject.common.utils.fromJson
import com.imsproject.common.utils.toJson
import com.imsproject.watch.utils.RestApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.java_websocket.exceptions.WebsocketNotConnectedException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URI
import java.util.stream.Collectors

// set these values to run the app locally
private const val RUNNING_LOCAL_GAME_SERVER : Boolean = true
private const val RUNNING_ON_EMULATOR : Boolean = true
private const val COMPUTER_NETWORK_IP = "192.168.0.104"

// ========== Constants ===========|
private const val TIMEOUT_MS = 2000L
private const val REMOTE_IP = "ims-project.cs.bgu.ac.il"
private val LOCAL_IP = if(RUNNING_ON_EMULATOR) "10.0.2.2" else COMPUTER_NETWORK_IP
val SERVER_IP = if (RUNNING_LOCAL_GAME_SERVER) LOCAL_IP else REMOTE_IP
private val WS_SCHEME = if (RUNNING_LOCAL_GAME_SERVER) "ws" else "wss"
private val REST_SCHEME = if (RUNNING_LOCAL_GAME_SERVER) "http" else "https"
private val SERVER_HTTP_PORT = if (RUNNING_LOCAL_GAME_SERVER) 8080 else 8640
const val SERVER_UDP_PORT = 8641
private const val TIME_SERVER_PORT = 8642
// ================================|

class MainModel (private val scope : CoroutineScope) {

    class CallbackNotSetException : Exception("Listener not set",null,true,false)
    class AlreadyConnectedException : Exception("Already connected",null,true,false)

    private lateinit var ws: WebSocketClient
    private lateinit var udp : UdpClient
    private var clientsClosed = true

    init{
        instance = this
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
    private var heartBeatListener : Job? = null

    // ======================================================================= |
    // ======================== Public Methods =============================== |
    // ======================================================================= |

    fun connectToServer() : Boolean {
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

        if(!ws.connectBlocking(10, java.util.concurrent.TimeUnit.SECONDS)){
            Log.e(TAG, "connectToServer: WebSocket connection timeout")
            return false // timeout
        }

        startHeartBeat()
        return true
    }

    /**
     * Establishes a connection to the game server via both WebSocket and UDP protocols.
     * @return the player ID if the connection is successful, or null if any step fails.
     * @throws AlreadyConnectedException if the client is already connected.
     */
    @Throws(AlreadyConnectedException::class)
    suspend fun enter(selectedId: String, force: Boolean = false) : String? {
        Log.d(TAG, "enter: Connecting to server with id $selectedId")

        if(clientsClosed) throw IllegalStateException("Clients are closed")
        stopHeartBeat()

        // start setup
        val enterType = if(force) GameRequest.Type.FORCE_ENTER else GameRequest.Type.ENTER
        val udpEnterCode: String
        try{
            udpEnterCode = (wsSetup(enterType, selectedId) ?: return null)
        } catch(e: AlreadyConnectedException){
            startHeartBeat()
            throw e
        }
        if(! udpSetup(udpEnterCode)) return null

        // connection established
        this.playerId = selectedId
        connected = true
        startListeners()
        startHeartBeat()

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
        stopHeartBeat()

        // start setup
        val udpEnterCode = wsSetup(GameRequest.Type.RECONNECT, playerId) ?: return false
        if(! udpSetup(udpEnterCode)) return false

        // connection established
        connected = true
        startListeners()
        startHeartBeat()

        Log.d(TAG, "reconnect: Connection established")
        return true
    }

    suspend fun exit() {
        connected = false
        sendTcp(GameRequest.builder(GameRequest.Type.EXIT).build().toJson())
        stopListeners()
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

    fun pingUdp() {
        if(connected.not()) return
        udp.send(GameAction.ping)
    }

    fun pingTcp() {
        if(connected.not()) return
        ws.send(GameRequest.ping)
    }

    suspend fun toggleReady() {
        val request = GameRequest.builder(GameRequest.Type.TOGGLE_READY).build().toJson()
        sendTcp(request)
    }

    suspend fun sendUserInput(timestamp: Long, sequenceNumber: Long, data: String? = null) {
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
        val timeServerUdp = UdpClient().apply{ init(); setTimeout(TIMEOUT_MS.toInt()) }
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
        return data.average().toLong()
    }

    fun uploadSessionEvents(sessionId: Int): Boolean {
        Log.d(TAG, "Uploading session events")
        val eventCollector = SessionEventCollectorImpl.getInstance()
        val events = eventCollector.getAllEvents().stream()
            .map { it.toCompressedJson() }
            .collect(Collectors.toList())

        val body = object {
            val sessionId = sessionId
            val events = events
        }

        val returned = RestApiClient()
            .withUri("$REST_SCHEME://$SERVER_IP:$SERVER_HTTP_PORT/data")
            .withBody(body.toJson())
            .withPost()
            .send()
        val response = fromJson<Response>(returned)
        if(response.success){
            Log.d(TAG, "uploadSessionEvents: Success")
            eventCollector.clearEvents()
        } else {
            Log.e(TAG, "uploadSessionEvents: Failed to upload events")
        }
        return response.success
    }

    suspend fun closeAllResources(){
        try{
            connected = false
            stopListeners()
            stopHeartBeat()
            ws.closeBlocking()
            udp.close()
            clientsClosed = true
        } catch(e: Exception){
            Log.e(TAG, "Failed to close resources", e)
        }
    }

    // ======================================================================= |
    // ======================== Private Methods ============================== |
    // ======================================================================= |

    private fun getNewClients(): Pair<WebSocketClient,UdpClient> {
        val ws = WebSocketClient(URI("$WS_SCHEME://$SERVER_IP:$SERVER_HTTP_PORT/ws"))
        ws.connectionLostTimeout = -1
        val udp = UdpClient()
        udp.remoteAddress = SERVER_IP
        udp.remotePort = SERVER_UDP_PORT
        udp.init()
        return ws to udp
    }

    @Throws(AlreadyConnectedException::class)
    private fun wsSetup (
        connectType: GameRequest.Type,
        id: String
    ): String? {

        val clearedCount = ws.clearPendingMessages()
        if(clearedCount > 0){
            Log.d(TAG, "wsSetup: Cleared $clearedCount pending messages")
        }

        // Send enter request and wait for response
        val enterRequest = GameRequest.builder(connectType)
            .playerId(id)
            .build().toJson()

        do {
            try{
                ws.send(enterRequest)
            } catch(e: WebsocketNotConnectedException){
                Log.e(TAG, "wsSetup: sending enter request failed",e)
                if(! ws.reconnectBlocking()){
                    Log.e(TAG, "wsSetup: reconnecting failed")
                    return null // reconnect failed
                }
                Log.d(TAG, "wsSetup: reconnected successful")
                continue
            }
            break
        } while(true)


        var enterResponse: GameRequest
        do {
            val response = ws.nextMessage(TIMEOUT_MS) ?: run {
                Log.e(TAG, "wsSetup: WebSocket $connectType request timeout")
                return null // timeout
            }
            try{
                enterResponse = fromJson(response)
            } catch(e: JsonParseException){
                Log.e(TAG, "wsSetup: Failed to parse WebSocket response", e)
                return null // invalid response
            }

            if(enterResponse.type == GameRequest.Type.ALREADY_CONNECTED){
                Log.d(TAG, "wsSetup: Already connected")
                throw AlreadyConnectedException()
            }

            if(enterResponse.type == GameRequest.Type.ERROR){
                Log.e(TAG, "wsSetup: Error response: ${enterResponse.data}")
                return null // error response
            }

            if(enterResponse.type != connectType){
                Log.d(TAG, "wsSetup: Ignoring message of type ${enterResponse.type}")
            }
        } while(enterResponse.type != connectType)

        val udpEnterCode = enterResponse.data?.get(0) ?: run {
            Log.e(TAG, "wsSetup: Missing UDP enter code")
            return null // invalid response
        }

       return udpEnterCode
    }

    private fun udpSetup(
        udpEnterCode: String
    ): Boolean {

        val clearedCount = udp.clearPendingMessages()
        if(clearedCount > 0){
            Log.d(TAG, "udpSetup: Cleared $clearedCount pending messages")
        }

        // send ENTER request with the code
        udp.send(
            GameAction.builder(GameAction.Type.ENTER)
                .data(udpEnterCode)
                .build().toString()
        )

        // === wait for confirmation === |
        udp.setTimeout(TIMEOUT_MS.toInt()) // set timeout

        var enterResponse: GameAction
        var message: String
        do {
            try{
                message = udp.receive()
            } catch (_: SocketTimeoutException) {
                Log.e(TAG, "udpSetup: UDP enter timeout")
                return false
            }
            try {
                enterResponse = GameAction.fromString(message)
            } catch (e: Exception) {
                Log.e(TAG, e.message, e)
                return false
            }
            if(enterResponse.type != GameAction.Type.ENTER){
                Log.d(TAG, "udpSetup: Ignoring message of type ${enterResponse.type}")
            }
        } while(enterResponse.type != GameAction.Type.ENTER)

        udp.setTimeout(0) // remove timeout
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
        Log.d(TAG, "Starting heartbeat listener")

        heartBeatListener = scope.launch(Dispatchers.IO){
            while(isActive){
                delay(5000)
                try{
                    ws.send(GameRequest.heartbeat)
                    udp.send(GameAction.heartbeat)
                } catch(e: WebsocketNotConnectedException){
                    executeCallback { tcpOnExceptionCallback(e) }
                    return@launch
                }
            }
        }
    }

    private suspend fun stopHeartBeat(){
        Log.d(TAG, "Stopping heartbeat listener")
        heartBeatListener?.cancel()
        heartBeatListener?.join()
        heartBeatListener = null
    }

    private fun startListeners() {
        Log.d(TAG, "Starting listeners")

        // UDP
        udpMessageListener = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val message = udp.receive()
                    val action = GameAction.fromString(message)
                    executeCallback { udpOnMessageCallback(action) }
                } catch (e: IOException) {
                    Log.e(TAG, "Failed to receive UDP message", e)
                    executeCallback { udpOnExceptionCallback(e) }
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
                    executeCallback { tcpOnMessageCallback(request) }
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

    private suspend fun sendTcp(message: String){
        try{
            ws.send(message)
        } catch (e : Exception){
            Log.e(TAG, "Failed to send TCP message", e)
            executeCallback { tcpOnExceptionCallback(e) }
        }
    }

    private suspend fun sendUdp(message: String){
        try{
            udp.send(message)
        } catch (e : Exception){
            Log.e(TAG, "Failed to send UDP message", e)
            executeCallback { udpOnExceptionCallback(e) }
        }
    }

    fun uploadSessionEvents(): Boolean {
        Log.d(TAG, "Uploading session events")
        val eventCollector = SessionEventCollectorImpl.getInstance()
        val events = eventCollector.getAllEvents().stream()
            .map { it.toCompressedJson() }
            .collect(Collectors.toList())

        val body = object {
            val events = events
        }
        val returned = RestApiClient()
            .withUri("$REST_SCHEME://$SERVER_IP:$SERVER_HTTP_PORT/data/lolk/lol")
            .withBody(body.toJson())
            .withPost()
            .send()
        val response = fromJson<Response>(returned)
        if(response.success){
            Log.d(TAG, "uploadSessionEvents: Success")
            eventCollector.clearEvents()
        } else {
            Log.e(TAG, "uploadSessionEvents: Failed to upload events")
        }
        return response.success
    }

    companion object {
        private const val TAG = "MainModel"
        lateinit var instance : MainModel
            private set
    }
}