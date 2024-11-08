package com.imsproject.webrtc.webrtc_client

import android.app.Application
import android.content.Context

private const val LOCALHOST = "10.0.2.2"


class Main : Application() {

    private val context : Context = this
    val wrtc : WebRTCClient = WebRTCClient("$LOCALHOST:8080/signaling", context)

    init{
        wrtc.peerMessageObserver = {
            println(it)
        }

        println("Connecting")
        try{
            if(wrtc.connectToServer()){
                println("Connected")
            } else {
                println("Not connected")
            }
        } catch (e : Exception){
            println(e)
        }
    }

    override fun onCreate() {
        super.onCreate()

    }
}