package com.imsproject.watch.view

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import com.imsproject.watch.DARK_BACKGROUND_COLOR
import com.imsproject.watch.viewmodel.MainViewModel
import com.imsproject.watch.viewmodel.MainViewModel.State
import androidx.wear.compose.material.ButtonDefaults
import com.imsproject.watch.COLUMN_PADDING
import com.imsproject.watch.GREEN_COLOR
import com.imsproject.watch.RED_COLOR
import com.imsproject.watch.SCREEN_HEIGHT
import com.imsproject.watch.SCREEN_WIDTH
import com.imsproject.watch.initGlobalValues
import com.imsproject.watch.textStyle


class MainActivity : ComponentActivity() {

    private val viewModel : MainViewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val metrics = getSystemService(WindowManager::class.java).currentWindowMetrics
        initGlobalValues(metrics.bounds.width(), metrics.bounds.height())

        setContent {
            Main(viewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        if(viewModel.state.value == State.IN_GAME){
            viewModel.afterGame()
        }
    }

    @Composable
    private fun Main(viewModel: MainViewModel){

        val state = viewModel.state.collectAsState().value

        when(state) {
            State.DISCONNECTED -> {
                BlankScreen()
                viewModel.connect()
            }

            State.CONNECTING -> ConnectingScreen()

            State.CONNECTED_NOT_IN_LOBBY -> {
                val userId = viewModel.playerId.collectAsState().value
                ConnectedNotInLobbyScreen(userId)
            }

            State.CONNECTED_IN_LOBBY -> {
                val userId = viewModel.playerId.collectAsState().value
                val lobbyId = viewModel.lobbyId.collectAsState().value
                val gameType = viewModel.gameType.collectAsState().value?.prettyName() ?: "Unknown game"
                val ready = viewModel.ready.collectAsState().value
                ConnectedInLobbyScreen(userId, lobbyId,gameType, ready){
                    viewModel.toggleReady()
                }
            }

            State.IN_GAME -> {
                BlankScreen()
                //TODO: register for activity result and call viewModel.afterGame()
                val intent = Intent(this, WaterRipplesActivity::class.java,)
//                registerForActivityResult() {
//                    viewModel.afterGame()
//                }
                startActivity(intent)
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
    fun BlankScreen() {
        MaterialTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DARK_BACKGROUND_COLOR),
                contentAlignment = Alignment.Center
            ){

            }
        }
    }

    @Composable
    private fun ConnectingScreen() {
        MaterialTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DARK_BACKGROUND_COLOR),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        strokeWidth = (SCREEN_WIDTH.toFloat() * 0.02f).dp,
                        modifier = Modifier.fillMaxSize(0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    BasicText(
                        text = "Connecting...",
                        style = textStyle
                    )
                }
            }
        }
    }

    @Composable
    private fun ConnectedNotInLobbyScreen(id: String) {
        MaterialTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DARK_BACKGROUND_COLOR),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .padding(COLUMN_PADDING)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ){
                    BasicText(
                        text = "Your ID: $id",
                        style = textStyle,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    BasicText(
                        text = "Waiting to be assigned\nto a lobby ...",
                        style = textStyle,
                    )
                }
            }
        }
    }

    @Composable
    private fun ConnectedInLobbyScreen(
        userId: String,
        lobbyId: String,
        gameType: String,
        ready: Boolean,
        onReady: () -> Unit
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "")
        val alpha = infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 0.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ), label = ""
        )

        MaterialTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DARK_BACKGROUND_COLOR),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .padding(COLUMN_PADDING-10.dp)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ){
                    BasicText(
                        text = "Your ID: $userId",
                        style = textStyle,
                    )
                    Spacer(modifier = Modifier.height(9.dp))
                    BasicText(
                        text = "Lobby ID: $lobbyId",
                        style = textStyle,
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    BasicText(
                        text = gameType,
                        style = textStyle,
                    )
                    Spacer(modifier = Modifier.height(9.dp))
                    BasicText(
                        text = "Status: ${if (ready) "Ready" else "Not Ready"}",
                        style = textStyle,
                    )
                    Spacer(modifier = Modifier.fillMaxHeight(0.2f))
                    // Button
                    Button(
                        onClick = { onReady() },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .fillMaxHeight(0.65f)

                        ,
                        border = ButtonDefaults.buttonBorder(
                            BorderStroke(2.dp,
                                if (! ready) GREEN_COLOR.copy(alpha = alpha.value)
                                else RED_COLOR.copy(alpha = alpha.value)),
                        )
                    ) {
                        BasicText(text = if (!ready) "Ready" else "Not Ready")
                    }
                }
            }
        }
    }

    @Preview(device = "id:wearos_large_round", apiLevel = 34)
    @Composable
    fun PreviewConnectingScreen() {
        ConnectingScreen()
    }

    @Preview(device = "id:wearos_large_round", apiLevel = 34)
    @Composable
    fun PreviewConnectedScreen() {
        ConnectedNotInLobbyScreen("123456")
    }

    @Preview(device = "id:wearos_large_round", apiLevel = 34)
    @Composable
    fun PreviewLobbyScreen() {
        ConnectedInLobbyScreen("123456", "ABC", "gameType", false) {}
    }

    @Preview(device = "id:wearos_large_round", apiLevel = 34)
    @Composable
    fun PreviewInGameScreen() {
        BlankScreen()
    }
}





