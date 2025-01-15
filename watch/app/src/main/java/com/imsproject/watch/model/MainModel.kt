package com.imsproject.watch.model

import android.util.Log
import com.google.gson.JsonParseException
import com.imsproject.common.etc.TimeRequest
import com.imsproject.common.gameserver.GameAction
import com.imsproject.common.gameserver.GameRequest
import com.imsproject.common.networking.UdpClient
import com.imsproject.common.networking.WebSocketClient
import com.imsproject.watch.model.MainModel.CallbackNotSetException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.java_websocket.exceptions.WebsocketNotConnectedException
import java.io.IOException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.URI
import java.util.concurrent.TimeUnit

// set these values to run the app locally
private const val RUNNING_LOCAL_GAME_SERVER : Boolean = false
private const val RUNNING_ON_EMULATOR : Boolean = false
private const val COMPUTER_NETWORK_IP = "192.168.0.104"

// ========== Constants ===========|
private const val TIMEOUT_MS = 2000L
private const val REMOTE_IP = "ims-project.cs.bgu.ac.il"
private val LOCAL_IP = if(RUNNING_ON_EMULATOR) "10.0.2.2" else COMPUTER_NETWORK_IP
private val SERVER_IP = if (RUNNING_LOCAL_GAME_SERVER) LOCAL_IP else REMOTE_IP
private val SCHEME = if (RUNNING_LOCAL_GAME_SERVER) "ws" else "wss"
private val SERVER_WS_PORT = if (RUNNING_LOCAL_GAME_SERVER) 8080 else 8640
private const val SERVER_UDP_PORT = 8641
private const val TIME_SERVER_PORT = 8642
// ================================|

class MainModel (private val scope : CoroutineScope) {

    class CallbackNotSetException : Exception("Listener not set",null,true,false)

    init{
        instance = this
    }

    private lateinit var ws: WebSocketClient
    private lateinit var udp : UdpClient
    private lateinit var timeServerUdp : UdpClient

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

    /**
     * Establishes a connection to the game server via both WebSocket and UDP protocols.
     * @return the player ID if the connection is successful, or null if any step fails.
     */
    fun connectToServer(id: String? = null) : String? {
        Log.d(TAG, "connectToServer: Connecting to server ${if(id!= null) "with id $id" else ""}")

        // start setup
        val (ws, udp, timeUdp) = getNewClients()
        val enterType = if(id!=null) GameRequest.Type.ENTER_WITH_ID else GameRequest.Type.ENTER
        val (playerId, udpEnterCode) = wsSetup(ws, enterType, id) ?: return null
        if(! udpSetup(udp, udpEnterCode)) return null

        // connection established
        this.ws = ws
        this.udp = udp
        this.timeServerUdp = timeUdp
        this.playerId = playerId
        connected = true
        initListeners()

        Log.d(TAG, "connectToServer: Connection established")
        return playerId
    }

    fun reconnect() : Boolean {
        Log.d(TAG, "reconnect: Reconnecting to server")
        val playerId = this.playerId ?: run {
            Log.e(TAG, "reconnect: Missing player id")
            return false
        }

        // start setup
        val (ws, udp, timeUdp) = getNewClients()
        val (_, udpEnterCode) = wsSetup(ws, GameRequest.Type.RECONNECT, playerId) ?: return false
        if(! udpSetup(udp, udpEnterCode)) return false

        // connection established
        this.ws = ws
        this.udp = udp
        this.timeServerUdp = timeUdp
        connected = true
        initListeners()

        Log.d(TAG, "reconnect: Connection established")
        return true
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

    /**
     * Sends a request to the time server to get the current time in milliseconds.
     *
     * @return The current time in milliseconds as reported by the time server.
     * @throws SocketTimeoutException If the request times out
     * @throws JsonParseException If the response from the time server cannot be parsed.
     * @throws IOException If there is an I/O error while sending or receiving the request.
     */
    fun getTimeServerCurrentTimeMillis(): Long {
        val request = TimeRequest.request(TimeRequest.Type.CURRENT_TIME_MILLIS).toJson()
        try{
            val startTime = System.currentTimeMillis()
            timeServerUdp.send(request)
            val response = timeServerUdp.receive()
            val timeDelta = System.currentTimeMillis() - startTime
            val timeResponse = TimeRequest.fromJson(response)
            return timeResponse.time!! - timeDelta / 2 // approximation
        } catch(e: SocketTimeoutException){
            Log.e(TAG, "Time request timeout", e)
            throw e
        } catch(e: JsonParseException){
            Log.e(TAG, "Failed to parse time response", e)
            throw e
        } catch (e: IOException){
            Log.e(TAG, "Failed to fetch time", e)
            throw e
        }
    }

    suspend fun closeAllResources(){
        try{
            connected = false
            onTcpMessage(null)
            onUdpMessage(null)
            onTcpError(null)
            udpMessageListener?.cancel()
            tcpMessageListener?.cancel()
            heartBeatListener?.cancel()
            ws.closeBlocking()
            ws.interrupt()
            udp.close()
            timeServerUdp.close()
            udpMessageListener?.join()
            tcpMessageListener?.join()
            heartBeatListener?.join()
        } catch(e: Exception){
            Log.e(TAG, "Failed to close resources", e)
        }
    }

    fun disconnect() {
        runBlocking {
            sendTcp(GameRequest.builder(GameRequest.Type.EXIT).build().toJson())
            closeAllResources()
        }
    }


    // ======================================================================= |
    // ======================== Private Methods ============================== |
    // ======================================================================= |

    private fun getNewClients(): Triple<WebSocketClient, UdpClient, UdpClient> {
        val ws = WebSocketClient(URI("$SCHEME://$SERVER_IP:$SERVER_WS_PORT/ws"))
        val udp = UdpClient()
        udp.remoteAddress = SERVER_IP
        udp.remotePort = SERVER_UDP_PORT
        udp.init()
        val timeUdp = UdpClient()
        timeUdp.remoteAddress = SERVER_IP
        timeUdp.remotePort = TIME_SERVER_PORT
        timeUdp.init()
        timeUdp.setTimeout(TIMEOUT_MS.toInt())
        return Triple(ws, udp, timeUdp)
    }

    private fun wsSetup (
        ws: WebSocketClient,
        connectType: GameRequest.Type,
        id: String?
    ): Pair<String,String>? {

        if(!ws.connectBlocking(TIMEOUT_MS, TimeUnit.MILLISECONDS)){
            Log.e(TAG, "wsSetup: WebSocket connection timeout")
            return null // timeout
        }

        // Send enter request and wait for response
        val enterRequest = GameRequest.builder(connectType)
            .apply { id?.let { playerId(it) } }
            .build().toJson()
        ws.send(enterRequest)
        val response = ws.nextMessage(TIMEOUT_MS) ?: run {
            Log.e(TAG, "wsSetup: WebSocket enter request timeout")
            return null // timeout
        }
        val enterResponse = GameRequest.fromJson(response)

        // validate response type
        if(enterResponse.type != connectType){
            Log.e(TAG, "wsSetup: Invalid WebSocket enter response type")
            return null // invalid response
        }

        // validate player id
        val playerId = id ?: (enterResponse.playerId ?: run {
            Log.e(TAG, "wsSetup: Missing player id in response")
            return null // invalid player id
        })

        val udpEnterCode = enterResponse.data?.get(0) ?: run {
            Log.e(TAG, "wsSetup: Missing UDP enter code")
            return null // invalid response
        }

       return playerId to udpEnterCode
    }

    private fun udpSetup(
        udp: UdpClient,
        udpEnterCode: String
    ): Boolean {

        // send ENTER request with the code
        udp.send(
            GameAction.builder(GameAction.Type.ENTER)
                .data(udpEnterCode)
                .build().toString()
        )

        // === wait for confirmation === |
        udp.setTimeout(TIMEOUT_MS.toInt()) // set timeout
        val confirmation: GameAction
        try {
            val message = udp.receive()
            confirmation = GameAction.fromString(message)
        }
        // message parsing error
        catch (e: IllegalArgumentException) {
            Log.e(TAG, e.message, e)
            return false
        }
        // timeout
        catch (_: SocketTimeoutException) {
            Log.e(TAG, "udpSetup: UDP confirmation timeout")
            return false
        }

        // validate confirmation
        if (confirmation.type != GameAction.Type.ENTER) {
            Log.e(TAG, "udpSetup: Invalid UDP enter response type")
            return false
        }
        // === end of confirmation === |
        udp.setTimeout(0) // remove timeout
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

    private fun initListeners() {

        Log.d(TAG, "initListeners: Initializing listeners")

        if(connected){
            Log.d(TAG, "initListeners: Canceling existing listeners")
            udpMessageListener?.cancel()
            tcpMessageListener?.cancel()
            heartBeatListener?.cancel()
        }

        // UDP
        udpMessageListener = scope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    if(! isActive) return@launch
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
                } catch (e: SocketException){
                    Log.e(TAG, "Failed to receive UDP message", e)
                    executeCallback { udpOnExceptionCallback(e) }
                    return@launch
                }
            }
        }

        tcpMessageListener = scope.launch(Dispatchers.IO){
            while(true){
                try{
                    if(! isActive) return@launch
                    val message = ws.nextMessageBlocking()
                    val request = GameRequest.fromJson(message)
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

        heartBeatListener = scope.launch(Dispatchers.IO){
            while(true){
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

        ws.onErrorListener = {
            scope.launch(Dispatchers.IO){
                executeCallback { tcpOnErrorCallback(it ?: Exception("Unknown error"))}
            }
        }
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

    companion object {
        private const val TAG = "MainModel"
        lateinit var instance : MainModel
            private set
    }
}