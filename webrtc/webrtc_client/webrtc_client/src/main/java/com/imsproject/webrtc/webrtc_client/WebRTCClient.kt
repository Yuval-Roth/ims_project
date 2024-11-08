package com.imsproject.webrtc.webrtc_client

import android.content.Context
import com.imsproject.utils.Response
import com.imsproject.utils.webrtc.WebRTCRequest
import org.webrtc.DataChannel
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SessionDescription
import java.net.URI

class WebRTCClient(val endpoint: String, val appContext: Context) {

    // private fields
    private lateinit var webSocket : WebSocketClient
    private lateinit var signalingClientId : String
    private lateinit var peerConnection : PeerConnection
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

    fun offer(targetId : String){

        // make sure the peerConnection is not initialized
        if (this::peerConnection.isInitialized){
            throw IllegalStateException("Peer connection is already initialized")
        }

        peerConnection = createPeerConnection() ?: throw IllegalStateException("Peer connection is null")

        peerConnection.createOffer(object: EmptySdpObserver(){
            override fun onCreateSuccess(desc: SessionDescription?) {
                peerConnection.setLocalDescription(this,desc)
            }

            override fun onSetSuccess() {
                val offer = WebRTCRequest.builder()
                    .setType(WebRTCRequest.Type.OFFER)
                    .setFrom(signalingClientId)
                    .setTo(targetId)
                    .setData(peerConnection.localDescription?.description)
                    .build()
                    .toJson()
                webSocket.send(offer)
            }
        },MediaConstraints())
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
            object : EmptyPeerConnectionObserver() {

                override fun onDataChannel(p0: DataChannel?) {
                    p0?.registerObserver(object : EmptyDataChannelObserver() {
                        override fun onMessage(buff: DataChannel.Buffer?) {
                            peerMessageObserver(String(buff?.data?.array()?: byteArrayOf()))
                        }
                    })
                }
            })
        return peerConnection
    }

}