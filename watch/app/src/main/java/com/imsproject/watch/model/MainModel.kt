package com.imsproject.watch.model

import android.util.Log
import com.google.gson.JsonParseException
import com.imsproject.common.gameServer.GameAction
import com.imsproject.common.gameServer.GameRequest
import com.imsproject.common.networking.UdpClient
import com.imsproject.common.networking.WebSocketClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URI
import java.util.concurrent.TimeUnit


// ========== Constants ===========|
private const val TIMEOUT_MS = 2000L
// private const val SERVER_IP = "10.0.2.2" // <------- USE THIS FOR EMULATOR AND SERVER ON LOCAL HOST
private const val SERVER_IP = "132.72.116.91" // <------- USE THIS FOR REMOTE SERVER
private const val SERVER_WS_PORT = 8640
private const val SERVER_UDP_PORT = 8641
// ================================|

class MainModel (private val vmScope : CoroutineScope) {

    private lateinit var ws: WebSocketClient
    private lateinit var udp : UdpClient

    var playerId : String? = null
        private set
    var connected = false
        private set

    private var udpMessageListener : Job? = null
    private var tcpMessageListener : Job? = null

    /**
     * Connects to the game server using WebSocket and UDP protocols.
     * @return the player ID if the connection is successful, or null if any step fails.
     */
    fun connectToServer() : String? {
        val ws = WebSocketClient(URI("ws://$SERVER_IP:$SERVER_WS_PORT/ws"))
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
            null // invalid response
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
            return null // invalid message
        }
        // timeout
        catch(e: SocketTimeoutException){
            Log.e(TAG, "connectToServer: UDP confirmation timeout")
            return null // timeout
        }

        // validate confirmation
        if(confirmation.type != GameAction.Type.ENTER){
            Log.e(TAG, "connectToServer: Invalid confirmation message")
            return null // invalid confirmation
        }
        udp.setTimeout(0) // reset timeout
        // === end of confirmation === |

        this.ws = ws
        this.udp = udp
        this.playerId = playerId
        connected = true

        // ================== End of UDP Setup ================== |
        // \/ \/ \/ \/ \/ \/ \/ \/ \/ \/ \/ \/ \/ \/ \/ \/ \/ \/ \/
        // ================= Connection Established ============= |

        return playerId
    }

    /**
     * Sets up a listener for incoming UDP messages.
     *
     * This function cancels any existing UDP message listener and creates a new one.
     *
     * When a message is received, it is parsed
     * into a [GameAction] object and passed to the provided callback function.
     * If an exception occurs during message reception or parsing, the provided
     * exception callback function is called.
     *
     * Either [IOException] or [JsonParseException] will be passed to the exception callback.
     *
     * @param callback A function to be called with the received `GameAction` object.
     * @param onException A function to be called when an exception occurs. Defaults to an empty function.
     */
    fun onUdpMessage(callback: (GameAction) -> Unit, onException: (Exception) -> Unit = {}) {
        val oldListener = udpMessageListener
        if(oldListener != null){
            oldListener.cancel()
            Log.d(TAG, "onUdpMessage: Canceling previous listener, creating new one")
        }
        val newListener = vmScope.launch(Dispatchers.IO){
            while(true){
                try{
                    val message = udp.receive()
                    val action = GameAction.fromString(message)
                    callback(action)
                } catch(e: IOException){
                    Log.e(TAG, "Failed to receive UDP message", e)
                    onException(e)
                } catch (e: JsonParseException) {
                    Log.e(TAG, "Failed to parse UDP message", e)
                    onException(e)
                }

            }
        }
        udpMessageListener = newListener
    }

    /**
     * Sets up a listener for incoming TCP messages.
     *
     * This function cancels any existing TCP message listener and creates a new one.
     *
     * When a message is received, it is parsed
     * into a [GameRequest] object and passed to the provided callback function.
     * If an exception occurs during message reception or parsing, the provided
     * exception callback function is called.
     *
     * Only [JsonParseException] will be passed to the exception callback.
     *
     * @param callback A function to be called with the received `GameRequest` object.
     * @param onException A function to be called when an exception occurs. Defaults to an empty function.
     */
    fun onTcpMessage(callback: (GameRequest) -> Unit, onException: (Exception) -> Unit = {}) {
        val oldListener = tcpMessageListener
        if(oldListener != null){
            oldListener.cancel()
            Log.d(TAG, "onTcpMessage: Canceling previous listener, creating new one")
        }
        val newListener = vmScope.launch(Dispatchers.IO){
            while(true){
                try{
                    val message = ws.nextMessageBlocking()
                    val request = GameRequest.fromJson(message)
                    callback(request)
                } catch(e: JsonParseException){
                    Log.e(TAG, "Failed to parse TCP message", e)
                    onException(e)
                }
            }
        }
        tcpMessageListener = newListener
    }

    /**
     * Sets up an error listener for the WebSocket client.
     *
     * This function assigns a listener to the WebSocket client's error events.
     * When an error occurs, the provided callback function is called with the exception.
     *
     * @param callback A function to be called with the exception that occurred.
     */
    fun onTcpError(callback: (Exception) -> Unit) {
        ws.onErrorListener = {
            vmScope.launch(Dispatchers.IO){
                callback(it ?: Exception("Unknown error"))
            }
        }
    }

    companion object {
        private const val TAG = "MainModel"
    }
}