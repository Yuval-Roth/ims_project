package com.imsproject.watch.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
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
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import com.imsproject.watch.viewmodel.MainViewModel
import com.imsproject.watch.viewmodel.MainViewModel.State

private const val DARK_BACKGROUND_COLOR = 0xFF333842
private const val LIGHT_BLUE_COLOR = 0xFFACC7F6

class MainActivity : ComponentActivity() {

    val viewModel : MainViewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Main(viewModel)
        }
    }
}

@Composable
private fun Main(viewModel: MainViewModel){

    val state = viewModel.state.collectAsState().value

    when(state) {
        State.DISCONNECTED -> viewModel.connect()

        State.CONNECTING -> ConnectingScreen()

        State.CONNECTED -> {
            val id = viewModel.id.collectAsState().value
            ConnectedScreen(id)
        }

        State.ERROR -> {
            val error = viewModel.error.collectAsState().value ?: "No error message"
            ErrorScreen(error) {
                viewModel.clearError()
            }
        }
    }
}

@Composable
private fun ConnectingScreen() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(DARK_BACKGROUND_COLOR)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(50.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                BasicText(
                    text = "Connecting...",
                    style = TextStyle(color = Color.White, fontSize = 14.sp)
                )
            }
        }
    }
}

@Composable
private fun ConnectedScreen(id: String) {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(DARK_BACKGROUND_COLOR)),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = "Your ID: $id",
                style = TextStyle(color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center),
            )
        }
    }
}

@Composable
private fun ErrorScreen(error: String, onDismiss: () -> Unit) {
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
                    style = TextStyle(color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally)
                        .padding(top=20.dp),


                )
                Spacer(modifier = Modifier.height(30.dp))
                Button(
                    onClick = { onDismiss() },
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    BasicText(
                        text = "Dismiss",
                        style = TextStyle(color = Color.White, fontSize = 14.sp)
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
fun PreviewConnectingScreen() {
    ConnectingScreen()
}

@Preview(device = "id:wearos_large_round", apiLevel = 34)
@Composable
fun PreviewConnectedScreen() {
    ConnectedScreen("123456")
}



