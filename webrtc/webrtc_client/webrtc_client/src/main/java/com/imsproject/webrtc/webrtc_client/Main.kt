package com.imsproject.webrtc.webrtc_client

import android.app.Application
import android.content.Context

private const val LOCALHOST = "10.0.2.2"


class Main : Application() {

    private val context : Context = this
    val wrtc : WebRTCClient = WebRTCClient("$LOCALHOST:8080/signaling", context)

    override fun onCreate() {
        super.onCreate()

        println("connected: ${wrtc.connectToServer()}")
        Thread{
            wrtc.offer("test")
            Thread.sleep(5000)
            wrtc.offer("test")
            Thread.sleep(5000)
            wrtc.offer("test")
            Thread.sleep(5000)
        }.start()
    }
}