package com.imsproject.watch.model

import android.util.Log
import com.google.gson.JsonParseException
import com.imsproject.common.etc.TimeRequest
import com.imsproject.common.gameServer.GameAction
import com.imsproject.common.gameServer.GameRequest
import com.imsproject.common.networking.UdpClient
import com.imsproject.common.networking.WebSocketClient
import com.imsproject.watch.model.MainModel.CallbackNotSetException
import com.imsproject.watch.viewmodel.WineGlassesViewModel.Angle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.java_websocket.exceptions.WebsocketNotConnectedException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URI
import java.util.concurrent.TimeUnit

// set these values to run the app locally
const val RUNNING_LOCAL_GAME_SERVER : Boolean = false
const val RUNNING_ON_EMULATOR : Boolean = false
const val COMPUTER_NETWORK_IP = "192.168.0.104"

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

    class CallbackNotSetException : Exception("Listener not set")

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

    private val defaultCallback  = { throw CallbackNotSetException() }
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
    fun connectToServer() : String? {
        val ws = WebSocketClient(URI("$SCHEME://$SERVER_IP:$SERVER_WS_PORT/ws"))
        val udp = UdpClient()
        udp.remoteAddress = SERVER_IP
        udp.remotePort = SERVER_UDP_PORT
        udp.init()

        //================== WebSocket Setup ================== |

        if(!ws.connectBlocking(TIMEOUT_MS, TimeUnit.MILLISECONDS)){
            Log.e(TAG, "connectToServer: WebSocket connection timeout")
            return null // timeout
        }

        // Send enter request and wait for response
        val enterRequest = GameRequest.builder(GameRequest.Type.ENTER)
            .build().toJson()
        ws.send(enterRequest)
        val response = ws.nextMessage(TIMEOUT_MS) ?: run {
            Log.e(TAG, "connectToServer: Connection timeout")
            return null // timeout
        }
        val enterResponse = GameRequest.fromJson(response)

        // validate response type
        if(enterResponse.type != GameRequest.Type.ENTER){
            Log.e(TAG, "connectToServer: Invalid response type")
            return null // invalid response
        }

        // validate player id
        val playerId = enterResponse.playerId ?: run {
            Log.e(TAG, "connectToServer: Invalid player id")
            return null // invalid player id
        }

        // ================== UDP Setup ================== |

        // get the ENTER code from the response from the WebSocket setup
        val udpEnterCode = enterResponse.data?.get(0) ?: run {
            Log.e(TAG, "connectToServer: Invalid response data")
            return null // invalid response
        }

        // send ENTER request with the code
        udp.send(GameAction.builder(GameAction.Type.ENTER)
            .data(udpEnterCode)
            .build().toString())

        // === wait for confirmation === |
        udp.setTimeout(TIMEOUT_MS.toInt()) // set timeout
        val confirmation: GameAction
        try {
            val message = udp.receive()
            confirmation = GameAction.fromString(message)
        }
        // message parsing error
        catch(e : IllegalArgumentException){
            Log.e(TAG,e.message,e)
            return null
        }
        // timeout
        catch(e: SocketTimeoutException){
            Log.e(TAG, "connectToServer: UDP confirmation timeout")
            return null
        }

        // validate confirmation
        if(confirmation.type != GameAction.Type.ENTER){
            Log.e(TAG, "connectToServer: Invalid confirmation message")
            return null
        }
        udp.setTimeout(0) // remove timeout
        // === end of confirmation === |

        // set up udp client for time server
        val timeServerUdp = UdpClient()
        timeServerUdp.remoteAddress = SERVER_IP
        timeServerUdp.remotePort = TIME_SERVER_PORT
        timeServerUdp.init()
        timeServerUdp.setTimeout(TIMEOUT_MS.toInt())

        this.ws = ws
        this.udp = udp
        this.timeServerUdp = timeServerUdp
        this.playerId = playerId
        connected = true

        // ================== End of UDP Setup ================== |
        // \/ \/ \/ \/ \/ \/ \/ \/ \/ \/ \/ \/ \/ \/ \/ \/ \/ \/ \/
        // ================= Connection Established ============= |

        initListeners()

        return playerId
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
            Log.d(TAG, "onTcpError: Listener canceled")
        } else {
            tcpOnErrorCallback = callback
            Log.d(TAG, "onTcpError: Listener set")
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

    suspend fun sendClick(timestamp: Long) {
        val request = GameAction.builder(GameAction.Type.CLICK)
            .timestamp(timestamp.toString())
            // add more things here if needed
            .build().toString()
        sendUdp(request)
    }

    suspend fun sendPosition(position : Position, timestamp: Long) {
        val request = GameAction.builder(GameAction.Type.POSITION)
            // add more things here if needed
            .data(position.toString())
            .timestamp(timestamp.toString())
            .build().toString()
        sendUdp(request)
    }

    suspend fun sendSyncRequest(timestamp: Long) {
        val request = GameAction.builder(GameAction.Type.SYNC_TIME)
            .timestamp(timestamp.toString())
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
            val halfRoundTripTime = timeDelta / 2
            return timeResponse.time!! - halfRoundTripTime // approximation
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

    private suspend fun executeCallback(action: suspend () -> Unit){
        while(true){
            try{
                action()
                break
            } catch(e: CallbackNotSetException){
                delay(100)
            }
        }
    }

    // ======================================================================= |
    // ======================== Private Methods ============================== |
    // ======================================================================= |

    private fun initListeners() {

        if(connected){
            udpMessageListener?.cancel()
            tcpMessageListener?.cancel()
            heartBeatListener?.cancel()
        }

        // UDP
        udpMessageListener = scope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    val message = udp.receive()
                    val action = GameAction.fromString(message)
                    executeCallback { udpOnMessageCallback(action) }
                } catch (e: IOException) {
                    Log.e(TAG, "Failed to receive UDP message", e)
                    executeCallback { udpOnExceptionCallback(e) }
                } catch (e: JsonParseException) {
                    Log.e(TAG, "Failed to parse UDP message", e)
                    executeCallback { udpOnExceptionCallback(e) }
                }
            }
        }

        tcpMessageListener = scope.launch(Dispatchers.IO){
            while(true){
                try{
                    val message = ws.nextMessageBlocking()
                    val request = GameRequest.fromJson(message)
                    executeCallback { tcpOnMessageCallback(request) }
                } catch(e: JsonParseException){
                    Log.e(TAG, "Failed to parse TCP message", e)
                    executeCallback { tcpOnExceptionCallback(e) }
                } catch(e: InterruptedException){
                    Log.d(TAG, "WebSocket listener interrupted")
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