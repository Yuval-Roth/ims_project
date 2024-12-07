package com.imsproject.poc_client.model

import android.util.Log
import com.imsproject.common.gameServer.GameAction
import com.imsproject.common.gameServer.GameRequest
import com.imsproject.common.networking.UdpClient
import com.imsproject.common.networking.WebSocketClient
import java.net.SocketTimeoutException
import java.net.URI
import java.util.concurrent.TimeUnit


// ========== Constants ===========|
private const val TIMEOUT_MS = 2000L
private const val SERVER_IP = "132.72.116.91"
private const val SERVER_WS_PORT = 8640
private const val SERVER_UDP_PORT = 8641
// ================================|

class MainModel () {

    private lateinit var ws: WebSocketClient
    private lateinit var udp : UdpClient

    var playerId : String? = null
        private set
    var connected = false
        private set


    // ========== ping related ===========|
    var onUdpPong : (Long) -> Unit = {}
    var onTcpPong : (Long) -> Unit = {}
    var udpPingSentTime : Long = 0
    var tcpPingSentTime : Long = 0
    // ===================================|


    /**
     * @return the player id if connection successful, if not, return null
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

        
        // set up listeners
        udp.onMessage = {
            val msg = GameAction.fromString(it)
            if(msg.type == GameAction.Type.PONG){
                onUdpPong(System.currentTimeMillis() - udpPingSentTime)
            }
        }
        udp.onListenerException = {
            Log.e(TAG, "connectToServer: UDP listener exception", it)
        }
        udp.startListener()

        Thread {
            while(true){
                val msg = ws.nextMessageBlocking()
                val pong = GameRequest.fromJson(msg)
                if(pong.type == GameRequest.Type.PONG){
                    onTcpPong(System.currentTimeMillis() - tcpPingSentTime)
                }
            }
        }.start()

        return playerId
    }
    
    fun pingUdp() {
        if(connected.not()) return
        udpPingSentTime = System.currentTimeMillis()
        udp.send(GameAction.ping())
    }

    fun pingTcp() {
        if(connected.not()) return
        tcpPingSentTime = System.currentTimeMillis()
        ws.send(GameRequest.ping())
    }

    companion object {
        private const val TAG = "MainModel"
    }
}