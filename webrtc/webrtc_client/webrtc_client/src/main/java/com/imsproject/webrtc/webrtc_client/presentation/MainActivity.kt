/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.imsproject.webrtc.webrtc_client.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices

import com.imsproject.webrtc.webrtc_client.R
import com.imsproject.webrtc.webrtc_client.WebSocketClient
import com.imsproject.webrtc.webrtc_client.presentation.theme.Webrtc_clientTheme
import org.java_websocket.handshake.Handshakedata
import java.net.URI

private const val LOCALHOST = "10.0.2.2"

class MainActivity : ComponentActivity() {

    val ws : WebSocketClient = WebSocketClient(URI("ws://$LOCALHOST:8080/signaling"))

    init{
        ws.onOpenListener = { hs : Handshakedata? -> println(hs?.content)}
        ws.onMessageListener = {println(it)}
        ws.onErrorListener = {println(it)}
        println("Connecting")
        try{
            if(ws.connectBlocking()){
                println("Connected")
            } else {
                println("Not connected")
            }
        } catch (e : Exception){
            println(e)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            WearApp("Android")
        }

        ws.send("Hello!")
    }

    override fun onDestroy() {
        super.onDestroy()
        ws.close()
    }
}

    @Composable
fun WearApp(greetingName: String) {
    Webrtc_clientTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            TimeText()
            Greeting(greetingName = greetingName)
        }
    }
}

@Composable
fun Greeting(greetingName: String) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        text = stringResource(R.string.hello_world, greetingName)
    )
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp("Preview Android")
}