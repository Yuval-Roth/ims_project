package com.imsproject.webrtc.webrtc_client

import android.content.Context
import com.imsproject.utils.Response
import com.imsproject.utils.webrtc.WebRTCRequest
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import java.net.URI
import java.nio.ByteBuffer

class WebRTCClient(val endpoint: String, val appContext: Context) {

    // private fields
    private lateinit var webSocket : WebSocketClient
    private lateinit var signalingClientId : String
    private var connectedToSignalingServer : Boolean = false
    // ====================================== |

    // public fields
    var peerMessageObserver : (String) -> Unit = {}
    // ====================================== |

    // constants
    private val iceServers = listOf(
        "stun:stun.l.google.com:19302",
        "stun:stun.l.google.com:5349",
        "stun:stun1.l.google.com:3478",
        "stun:stun1.l.google.com:5349",
        "stun:stun2.l.google.com:19302",
        "stun:stun2.l.google.com:5349",
        "stun:stun3.l.google.com:3478",
        "stun:stun3.l.google.com:5349",
        "stun:stun4.l.google.com:19302",
        "stun:stun4.l.google.com:5349"
    ).map { PeerConnection.IceServer.builder(it).createIceServer() }

    // ====================================== |


    fun connectToServer() : Boolean {
        if(connectedToSignalingServer){
            throw IllegalStateException("Already connected to signaling server")
        }

        // create a new WebSocketClient instance
        webSocket = WebSocketClient(URI("ws://$endpoint"))

        // connect to the signaling server
        if (! webSocket.connectBlocking()){
            return false
        }

        // send an enter request to the signaling server
        val request = WebRTCRequest.builder()
            .setType(WebRTCRequest.Type.ENTER)
            .build()
            .toJson()
        webSocket.send(request)

        // wait for the response
        var response = webSocket.nextMessageBlocking()

        try{
            // parse the response
            val responseObj = Response.fromJson(response)

            if (responseObj.success()){
                signalingClientId = responseObj.payload().first() // get the client id
                connectedToSignalingServer = true
                return true
            }

        } catch (e : Exception){
            // TODO: Handle this exception and log it
            e.printStackTrace()
        }

        // connection not successful
        webSocket.close()
        return false
    }

    private fun initPeerConnection(){
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(appContext)
                .setEnableInternalTracer(true)
                .createInitializationOptions())
    }

    private fun createPeerConnectionFactory() : PeerConnectionFactory {
        return PeerConnectionFactory.builder()
            .setOptions(PeerConnectionFactory.Options())
            .createPeerConnectionFactory()
    }

    private fun createPeerConnection() : PeerConnection? {
        val config = PeerConnection.RTCConfiguration(iceServers)

        val peerConnection = createPeerConnectionFactory().createPeerConnection(
            config,
            object : PeerConnection.Observer {

                override fun onDataChannel(p0: DataChannel?) {
                    p0?.registerObserver(object : DataChannel.Observer {
                        override fun onMessage(buffer: DataChannel.Buffer) {
                            val message = String(buffer.data.array())
                            // Handle incoming message here
                            println("Received message: $message")
                        }

                        override fun onStateChange() {
                            println("DataChannel state: ${p0.state()}")
                        }

                        override fun onBufferedAmountChange(previousAmount: Long) {
                            // Handle buffered amount change if needed
                        }
                    })
                }

                override fun onSignalingChange(p0: PeerConnection.SignalingState) {}
                override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {}
                override fun onIceConnectionReceivingChange(p0: Boolean) {}
                override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
                override fun onIceCandidate(p0: IceCandidate?) {}
                override fun onIceCandidatesRemoved(p0: Array<out IceCandidate?>?) {}
                override fun onAddStream(p0: MediaStream?) {}
                override fun onRemoveStream(p0: MediaStream?) {}
                override fun onRenegotiationNeeded() {}
                override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream?>?) {}
            })

        // Create the DataChannel with desired configuration
        val dataChannelInit = DataChannel.Init()
        val dataChannel = peerConnection?.createDataChannel("textChannel", dataChannelInit)

        // Set up DataChannel observer to listen for outgoing messages
        dataChannel?.registerObserver(object : DataChannel.Observer {
            override fun onMessage(buffer: DataChannel.Buffer) {
                peerMessageObserver(String(buffer.data.array()))
            }

            override fun onStateChange() {
                if (dataChannel.state() == DataChannel.State.OPEN) {
                    // Send a text message when the channel is open
                    val message = "Hello from the other side!"
                    val buffer = DataChannel.Buffer(ByteBuffer.wrap(message.toByteArray()), false)
                    dataChannel.send(buffer)
                }
            }

            override fun onBufferedAmountChange(previousAmount: Long) {}
        })

        return peerConnection
    }

}