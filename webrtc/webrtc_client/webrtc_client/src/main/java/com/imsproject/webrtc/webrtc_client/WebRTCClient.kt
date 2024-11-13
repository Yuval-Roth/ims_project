package com.imsproject.webrtc.webrtc_client

import android.content.Context
import android.util.Log
import com.imsproject.utils.JsonUtils
import com.imsproject.utils.WebSocketClient
import com.imsproject.utils.webrtc.Candidate
import com.imsproject.utils.webrtc.WebRTCRequest
import com.imsproject.utils.webrtc.WebRTCRequest.Type
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import java.net.URI

private const val TAG = "WebRTCClient"

class WebRTCClient(val endpoint: String, val appContext: Context) {

    //TODO: Figure out how to start the peer connection after ice candidates are exchanged

    enum class State {
        IDLE,
        CONSIDERING_OFFER,
        WAITING_FOR_ANSWER,
        NEGOTIATING_ICE,
        CONNECTED
    }

    private lateinit var webSocket : WebSocketClient
    private lateinit var signalingClientId : String
    private lateinit var peerConnection : PeerConnection
    private lateinit var webSocketThread : Thread
    private lateinit var peerMessageThread : Thread
    private var webSocketLoopRunning : Boolean = false
    private var peerMessageLoopRunning : Boolean = false
    private var connectedToSignalingServer : Boolean = false
    private var state : State = State.IDLE
    private var currentTarget : String = ""
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


    // ============================================================================= |
    // ============================== CALLBACKS ==================================== |
    // ============================================================================= |

    /**
     * Callback that will be called when the client is connected to the signaling server
     */
    var onServerConnected : () -> Unit = {}

    /**
     * Callback that will be called when a peer is connected
     */
    var onPeerConnected : () -> Unit = {}

    /**
     * Callback that will be called when a message is received from another client
     */
    var onPeerMessage : (String) -> Unit = {}

    /**
     * Callback that will be called when an offer is received from another client
     */
    var onOffer : (WebRTCRequest) -> Unit = {}

    /**
     * Callback that will be called when the target client accepts the offer
     */
    var onAnswer : (WebRTCRequest) -> Unit = {}

    /**
     * Callback that will be called when an error occurs
     */
    var onError : (WebRTCRequest) -> Unit = {}

    /**
     * Callback that will be called when the target client is busy
     */
    var onBusy : (WebRTCRequest) -> Unit = {}

    /**
     * Callback that will be called when the target client declines the offer
     */
    var onDecline : (WebRTCRequest) -> Unit = {}


    // ============================================================================= |
    // ============================== WEBRTC PROTOCOL ============================== |
    // ============================================================================= |

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
        WebRTCRequest.builder()
            .setType(Type.ENTER)
            .build()
            .toJson()
            .also { webSocket.send(it) }

        // wait for the response
        var response = webSocket.nextMessageBlocking()

        try{
            // parse the response
            val responseObj = WebRTCRequest.fromJson(response)

            if (responseObj.type() == Type.ENTER){
                signalingClientId = responseObj.to() // get the client id
                connectedToSignalingServer = true
                initPeerConnection()
                startWebSocketLoop()
                onServerConnected() // notify the user
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

    /**
     * Offer a connection to another client
     */
    fun offer(targetId : String){
        validateSignalingConditions()

        if(state != State.IDLE){
            throw IllegalStateException("Cannot offer while in state: $state")
        }

        peerConnection.createOffer(object: EmptySdpObserver(){
            override fun onCreateSuccess(desc: SessionDescription?) {
                peerConnection.setLocalDescription(this,desc)
            }

            override fun onSetSuccess() {
                sendToWebSocket(Type.OFFER, targetId, peerConnection.localDescription?.description!!)
                currentTarget = targetId
                state = State.WAITING_FOR_ANSWER
            }
        },MediaConstraints())
    }

    /**
     * Accept an offer from another client
     */
    fun answer(targetId : String){
        validateSignalingConditions()

        if(state != State.CONSIDERING_OFFER){
            throw IllegalStateException("Cannot answer offer while in state: $state")
        }

        peerConnection.createAnswer(object: EmptySdpObserver(){
            override fun onCreateSuccess(desc: SessionDescription?) {
                peerConnection.setLocalDescription(this,desc)
            }

            override fun onSetSuccess() {
                sendToWebSocket(Type.ANSWER, targetId, peerConnection.localDescription?.description!!)
                currentTarget = targetId
                state = State.NEGOTIATING_ICE
            }
        },MediaConstraints())
    }

    /**
     * Decline an offer from another client
     */
    fun decline(){
        validateSignalingConditions()

        if(state != State.CONSIDERING_OFFER){
            throw IllegalStateException("Cannot decline offer while in state: $state")
        }

        sendToWebSocket(Type.DECLINE, currentTarget)
        currentTarget = ""
        state = State.IDLE
    }

    private fun handleOffer(request: WebRTCRequest) {

        if(state != State.IDLE){
            Log.d(TAG, "Received offer while in state: $state")
            sendToWebSocket(Type.BUSY, request.from())
            return
        }

        state = State.CONSIDERING_OFFER
        currentTarget = request.from()
        onOffer(request) // notify the user
    }

    private fun handleAnswer(request: WebRTCRequest) {

        if(state != State.WAITING_FOR_ANSWER){
            Log.d(TAG, "Received answer while in state: $state")
            sendToWebSocket(Type.ERROR, request.from(), "Received answer while in state: $state")
            return
        }

        if(request.from() != currentTarget){
            Log.d(TAG, "Received answer from unexpected source: ${request.from()}")
            sendToWebSocket(Type.ERROR, request.from(), "Received answer from unexpected source: ${request.from()}")
            return
        }

        currentTarget = request.from()
        state = State.NEGOTIATING_ICE

        // set the remote description
        SessionDescription(SessionDescription.Type.ANSWER, request.data()).also {
            peerConnection.setRemoteDescription(EmptySdpObserver(),it)
        }

        onAnswer(request) // notify the user
    }

    private fun handleCandidate(request: WebRTCRequest) {

        if(state != State.NEGOTIATING_ICE){
            Log.d(TAG, "Received ice candidates while in state: $state")
            sendToWebSocket(Type.ERROR, request.from(), "Received ice candidates while in state: $state")
            return
        }

        if(request.from() != currentTarget){
            Log.d(TAG, "Received ice candidates from unexpected source: ${request.from()}")
            sendToWebSocket(Type.ERROR, request.from(), "Received ice candidates from unexpected source: ${request.from()}")
            return
        }

        // add the ice candidate
        request.data().let{
            JsonUtils.deserialize<Candidate>(it, Candidate::class.java)
        }.also {
            peerConnection.addIceCandidate(IceCandidate(it.sdpMid,it.sdpMLineIndex,it.sdp))
        }
    }

    private fun handleDecline(request: WebRTCRequest) {
        if(state != State.WAITING_FOR_ANSWER){
            Log.d(TAG, "Received decline while in state: $state")
            sendToWebSocket(Type.ERROR, request.from(), "Received decline while in state: $state")
            return
        }

        if(request.from() != currentTarget){
            Log.d(TAG, "Received decline from unexpected source: ${request.from()}")
            sendToWebSocket(Type.ERROR, request.from(), "Received decline from unexpected source: ${request.from()}")
            return
        }

        state = State.IDLE
        currentTarget = ""
        onDecline(request) // notify the user
    }

    private fun handleBusy(request: WebRTCRequest) {
        if(state != State.WAITING_FOR_ANSWER){
            Log.d(TAG, "Received busy while in state: $state")
            sendToWebSocket(Type.ERROR, request.from(), "Received busy while in state: $state")
            return
        }

        if(request.from() != currentTarget){
            Log.d(TAG, "Received busy from unexpected source: ${request.from()}")
            sendToWebSocket(Type.ERROR, request.from(), "Received busy from unexpected source: ${request.from()}")
            return
        }

        state = State.IDLE
        currentTarget = ""
        onBusy(request) // notify the user
    }

    private fun handleError(request: WebRTCRequest) {
        Log.e(TAG, "Received error from ${request.from()}: ${request.data()}")
        if(currentTarget == request.from()){
            state = State.IDLE
            currentTarget = ""
            peerConnection = createPeerConnection()!! // reset the peer connection
        }
        onError(request) // notify the user
    }


    // ============================================================================= |
    // ============================== WEBSOCKET ==================================== |
    // ============================================================================= |


    private fun webSocketLoop() {
        while (webSocketLoopRunning) {
            // block until a message is received
            val message = webSocket.nextMessageBlocking()

            // check if the loop is still running
            if(webSocketLoopRunning.not()){
                return
            }

            // parse the message
            val request = WebRTCRequest.fromJson(message)

            // handle the request
            when (request.type()) {
                Type.OFFER -> handleOffer(request)
                Type.ANSWER -> handleAnswer(request)
                Type.ICE_CANDIDATES -> handleCandidate(request)
                Type.DECLINE -> handleDecline(request)
                Type.ERROR -> handleError(request)
                Type.BUSY -> handleBusy(request)
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
        }.apply { start() }
    }

    private fun stopWebSocketLoop() {
        if(webSocketLoopRunning.not()){
            throw IllegalStateException("WebSocket loop not running")
        }

        webSocketLoopRunning = false
        webSocket.interrupt()
        webSocketThread.join()
    }

    private fun sendToWebSocket(type: Type, to: String = "", data: String = "") {
        WebRTCRequest.builder()
            .setType(type)
            .setFrom(signalingClientId)
            .setTo(to)
            .setData(data)
            .build()
            .toJson()
            .also { webSocket.send(it) }
    }

    // ============================================================================= |
    // ======================== PEER CONNECTION METHODS ============================ |
    // ============================================================================= |

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

                override fun onIceCandidate(p0: IceCandidate?) {
                    val candidate = Candidate(
                        p0?.sdp,
                        p0?.sdpMid,
                        p0!!.sdpMLineIndex
                    )
                    println("onIceCandidate: $candidate")

                    if(state == State.NEGOTIATING_ICE){
                        sendToWebSocket(Type.ICE_CANDIDATES, currentTarget, JsonUtils.serialize(candidate))
                    }
                }

                override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
                    println("onIceGatheringChange: $p0")
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

    private fun establishConnection(){
        if(state != State.NEGOTIATING_ICE){
            throw IllegalStateException("Cannot establish connection while in state: $state")
        }

        state = State.CONNECTED
        onPeerConnected()
    }

    // ============================================================================= |
    // ============================ HELPER METHODS ================================= |
    // ============================================================================= |

    private fun validateSignalingConditions() {
        if (!connectedToSignalingServer) {
            throw IllegalStateException("Not connected to signaling server")
        }

        if (!this::peerConnection.isInitialized) {
            throw IllegalStateException("Peer connection not initialized")
        }
    }

    // ============================================================================= |
    // ============================== INNER CLASSES ================================ |
    // ============================================================================= |

    open class EmptyDataChannelObserver : DataChannel.Observer {
        override fun onBufferedAmountChange(p0: Long) {}
        override fun onStateChange() {}
        override fun onMessage(p0: DataChannel.Buffer?) {}
    }

    open class EmptyPeerConnectionObserver : PeerConnection.Observer {
        override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
        override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {}
        override fun onIceConnectionReceivingChange(p0: Boolean) {}
        override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
        override fun onIceCandidate(p0: IceCandidate?) {}
        override fun onIceCandidatesRemoved(p0: Array<out IceCandidate?>?) {}
        override fun onAddStream(p0: MediaStream?) {}
        override fun onRemoveStream(p0: MediaStream?) {}
        override fun onDataChannel(p0: DataChannel?) {}
        override fun onRenegotiationNeeded() {}
        override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream?>?) {}
    }

    open class EmptySdpObserver : SdpObserver {
        override fun onCreateSuccess(p0: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(p0: String?) {}
        override fun onSetFailure(p0: String?) {}
    }
}