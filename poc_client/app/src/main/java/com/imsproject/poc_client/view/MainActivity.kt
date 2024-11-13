package com.imsproject.poc_client.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Picker
import androidx.wear.compose.material.PickerState
import com.imsproject.poc_client.viewmodel.MainViewModel
import com.imsproject.poc_client.viewmodel.MainViewModel.State
import java.util.LinkedList

private const val DARK_BACKGROUND_COLOR = 0xFF333842
private const val LIGHT_BLUE_COLOR = 0xFFACC7F6

class MainActivity : ComponentActivity() {

    val viewModel : MainViewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen(viewModel)
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel){

    val state = viewModel.state.collectAsState().value

    when(state) {
        State.DISCONNECTED -> ConnectButtonScreen { viewModel.connect() }

        State.CONNECTING -> ConnectingScreen()

        State.CONNECTED -> ConnectedScreen(
            viewModel.id.collectAsState().value,
            viewModel.tcpPing.collectAsState().value,
            viewModel.udpPing.collectAsState().value,
            viewModel.udpPacketsLost.collectAsState().value
        )

        State.ERROR -> ErrorScreen(viewModel.error.collectAsState().value!!) {
            viewModel.clearError()
        }
    }
}

@Composable
fun ConnectButtonScreen(onClick: () -> Unit) {
    // Using MaterialTheme for a polished look
    MaterialTheme {

        // Full-screen Box layout
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(DARK_BACKGROUND_COLOR)), // Dark background for clarity

            contentAlignment = Alignment.Center // Center everything in the box
        ) {
            // Circular button in the center
            Button(
                onClick =  {onClick()} ,
                shape = CircleShape, // Circular shape
                modifier = Modifier
                    .size(125.dp)// Button size (height and width)
            ) {
                BasicText(
                    text = "Connect",
                    style = TextStyle(color = Color.White, fontSize = 20.sp)
                )
            }

        }
    }
}

@Composable
fun ConnectingScreen() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(DARK_BACKGROUND_COLOR)), // Light background for clarity
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(50.dp) // Appropriately sized for WearOS
                )
                Spacer(modifier = Modifier.height(16.dp)) // Space between indicator and text
                BasicText(
                    text = "Connecting...",
                    style = TextStyle(color = Color.White, fontSize = 14.sp)
                )
            }
        }
    }
}

@Composable
fun ConnectedScreen(id: String, tcpPing : Long, udpPing : Long, udpPacketsLost : Int) {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(DARK_BACKGROUND_COLOR)),
            contentAlignment = Alignment.Center,
        ) {
            Column (
                modifier = Modifier
                    .padding(15.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,

            ) {
                BasicText(
                    text = "Your ID: $id",
                    style = TextStyle(color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center),
                )
                Spacer(modifier = Modifier.height(15.dp))
                BasicText(
                    text = "TCP ping: $tcpPing",
                    style = TextStyle(color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center),
                )
                Spacer(modifier = Modifier.height(15.dp))
                BasicText(
                    text = "UDP ping: $udpPing" + if(udpPacketsLost > 0) " ($udpPacketsLost)" else "",
                    style = TextStyle(color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center),
                )
            }
        }
    }
}

@Composable
private fun SimplePicker(
    state: PickerState,
    onClick: (String) -> Unit,
    items: LinkedList<String>
) {
    Picker(
        modifier = Modifier
            .size(75.dp, 125.dp)
            .background(Color(LIGHT_BLUE_COLOR), CircleShape)
            .border(3.dp, Color.White, CircleShape),
        state = state,
        gradientRatio = 0f,
        separation = 5.dp,
        contentDescription = "playerID",
    ) {
        Button(
            onClick = { onClick(items[state.selectedOption]) },
            modifier = Modifier
                .size(70.dp, 40.dp)
                .background(Color.Transparent),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent)

        ) {
            BasicText(
                text = items[it],
                style = TextStyle(color = Color.White, fontSize = 30.sp),
            )
        }

    }
}

@Composable
fun ErrorScreen(error: String, onDismiss: () -> Unit) {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(DARK_BACKGROUND_COLOR)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(30.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BasicText(
                    text = "Error: $error",
                    style = TextStyle(color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center), // Smaller font size
                    // center the text
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally)

                )
                Spacer(modifier = Modifier.height(30.dp)) // Reduced spacing
                Button(
                    onClick = { onDismiss() },
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    BasicText(
                        text = "Dismiss",
                        style = TextStyle(color = Color.White, fontSize = 14.sp) // Smaller font size
                    )
                }
            }
        }
    }
}

@Preview(device = "id:wearos_large_round", apiLevel = 34)
@Composable
fun PreviewErrorScreen() {
    ErrorScreen("Failed to connect to server"){}
}

@Preview(device = "id:wearos_large_round", apiLevel = 34)
@Composable
fun PreviewConnectButton() {
    ConnectButtonScreen {  }
}

@Preview(device = "id:wearos_large_round", apiLevel = 34)
@Composable
fun PreviewConnectingScreen() {
    ConnectingScreen()
}

@Preview(device = "id:wearos_large_round", apiLevel = 34)
@Composable
fun PreviewConnectedScreen() {
    ConnectedScreen("123456",10L,10L,5)
}




