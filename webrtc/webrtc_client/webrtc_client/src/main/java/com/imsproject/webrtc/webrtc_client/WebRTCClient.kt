package com.imsproject.webrtc.webrtc_client

import android.content.Context
import android.util.Log
import com.imsproject.utils.Response
import com.imsproject.utils.webrtc.Candidate
import com.imsproject.utils.webrtc.WebRTCRequest
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SessionDescription
import java.net.URI

private const val TAG = "WebRTCClient"

class WebRTCClient(val endpoint: String, val appContext: Context) {

    enum class State {
        IDLE,
        CONSIDERING_OFFER,
        WAITING_FOR_ANSWER,
        NEGOTIATING_ICE,
        CONNECTED
    }

    // private fields
    private lateinit var webSocket : WebSocketClient
    private lateinit var signalingClientId : String
    private lateinit var peerConnection : PeerConnection
    private lateinit var webSocketThread : Thread
    private lateinit var peerMessageThread : Thread
    private var webSocketLoopRunning : Boolean = false
    private var peerMessageLoopRunning : Boolean = false
    private var connectedToSignalingServer : Boolean = false
    private var state : State = State.IDLE
    // ====================================== |

    // public fields
    var onPeerMessage : (String) -> Unit = {}
    var onOffer : (WebRTCRequest) -> Unit = {}
    // ====================================== |

    // constants
    private val iceServers = listOf(
        "stun:stun.l.google.com:19302",
//        "stun:stun.l.google.com:5349",
//        "stun:stun1.l.google.com:3478",
//        "stun:stun1.l.google.com:5349",
//        "stun:stun2.l.google.com:19302",
//        "stun:stun2.l.google.com:5349",
//        "stun:stun3.l.google.com:3478",
//        "stun:stun3.l.google.com:5349",
//        "stun:stun4.l.google.com:19302",
//        "stun:stun4.l.google.com:5349"
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
                initPeerConnection()
                startWebSocketLoop()
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

        if(!connectedToSignalingServer){
            throw IllegalStateException("Not connected to signaling server")
        }

        if(!this::peerConnection.isInitialized){
            throw IllegalStateException("Peer connection not initialized")
        }

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
                state = State.WAITING_FOR_ANSWER
            }
        },MediaConstraints())
    }

    fun answer(targetId : String){

        if(!connectedToSignalingServer){
            throw IllegalStateException("Not connected to signaling server")
        }

        if(!this::peerConnection.isInitialized){
            throw IllegalStateException("Peer connection not initialized")
        }

        peerConnection.createAnswer(object: EmptySdpObserver(){
            override fun onCreateSuccess(desc: SessionDescription?) {
                peerConnection.setLocalDescription(this,desc)
            }

            override fun onSetSuccess() {
                val answer = WebRTCRequest.builder()
                    .setType(WebRTCRequest.Type.ANSWER)
                    .setFrom(signalingClientId)
                    .setTo(targetId)
                    .setData(peerConnection.localDescription?.description)
                    .build()
                    .toJson()
                webSocket.send(answer)
            }
        },MediaConstraints())
    }

    private fun addIceCandidate(candidate : Candidate){
        val iceCandidate = IceCandidate(
            candidate.sdpMid,
            candidate.sdpMLineIndex,
            candidate.candidate
        )
        peerConnection.addIceCandidate(iceCandidate)
    }

    private fun initPeerConnection(){
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(appContext)
                .setEnableInternalTracer(true)
                .createInitializationOptions())

        peerConnection = createPeerConnection() ?: throw IllegalStateException("Peer connection is null")
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
                            onPeerMessage(String(buff?.data?.array()?: byteArrayOf()))
                        }
                    })
                }

//                override fun onIceCandidate(p0: IceCandidate?) {
//                    peerConnection.addIceCandidate(p0)
//                }

                override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
                    println("ICE connection state: $p0")
                }
            })

        peerConnection?.createDataChannel("dataChannel", DataChannel.Init())
            ?.registerObserver(object : EmptyDataChannelObserver() {
                override fun onMessage(buff: DataChannel.Buffer?) {
                    onPeerMessage(String(buff?.data?.array()?: byteArrayOf()))
                }
            })

        return peerConnection
    }

    private fun webSocketLoop() {
        while (webSocketLoopRunning) {
            val message = webSocket.nextMessageBlocking()
            if(webSocketLoopRunning.not()){
                return
            }
            val request = WebRTCRequest.fromJson(message)

            when (request.type()) {
                WebRTCRequest.Type.OFFER -> {
                    if(state != State.IDLE){
                        Log.d(TAG, "Received offer while in state: $state")
                        continue
                    }
                    state = State.CONSIDERING_OFFER
                    onOffer(request)
                }
                WebRTCRequest.Type.ANSWER -> {
                    if(state != State.WAITING_FOR_ANSWER){
                        Log.d(TAG, "Received answer while in state: $state")
                        continue
                    }
                    state = State.NEGOTIATING_ICE
                    // handle answer
                }
                WebRTCRequest.Type.ICE_CANDIDATES -> {
                    if(state != State.NEGOTIATING_ICE){
                        Log.d(TAG, "Received ice candidates while in state: $state")
                        continue
                    }
                    // handle candidate
                }
                else -> {
                    Log.e(TAG, "Received unexpected request type: ${request.type()}")
                    throw IllegalStateException("Received unexpected request type: ${request.type()}")
                }
            }
        }
    }

    private fun startWebSocketLoop() {
        if(webSocketLoopRunning){
            throw IllegalStateException("WebSocket loop already running")
        }

        webSocketLoopRunning = true
        webSocketThread = Thread {
            webSocketLoop()
        }
        webSocketThread.start()
    }

    private fun stopWebSocketLoop() {
        if(webSocketLoopRunning.not()){
            throw IllegalStateException("WebSocket loop not running")
        }

        webSocketLoopRunning = false
        webSocket.interrupt()
        webSocketThread.join()
    }
}