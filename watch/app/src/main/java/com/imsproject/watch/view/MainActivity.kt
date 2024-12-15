package com.imsproject.watch.view

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import com.imsproject.watch.COLUMN_PADDING
import com.imsproject.watch.DARK_BACKGROUND_COLOR
import com.imsproject.watch.GREEN_COLOR
import com.imsproject.watch.RED_COLOR
import com.imsproject.watch.SCREEN_HEIGHT
import com.imsproject.watch.SCREEN_WIDTH
import com.imsproject.watch.TEXT_SIZE
import com.imsproject.watch.initGlobalValues
import com.imsproject.watch.textStyle
import com.imsproject.watch.view.contracts.WaterRipplesResultContract
import com.imsproject.watch.viewmodel.MainViewModel
import com.imsproject.watch.viewmodel.MainViewModel.State


class MainActivity : ComponentActivity() {

    private val viewModel : MainViewModel by viewModels<MainViewModel>()
    private lateinit var waterRipples: ActivityResultLauncher<Unit>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val metrics = getSystemService(WindowManager::class.java).currentWindowMetrics
        initGlobalValues(metrics.bounds.width(), metrics.bounds.height())
        registerActivities()
        setContent {
            Main(viewModel)
        }
    }

    private fun registerActivities(){
        waterRipples = registerForActivityResult(WaterRipplesResultContract()) {
            viewModel.afterGame(it)
        }
    }

    override fun onResume() {
        super.onResume()
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
                waterRipples.launch(Unit)
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
    private fun BlankScreen() {
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
                        .padding(COLUMN_PADDING - 10.dp)
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

    @Composable
    private fun ErrorScreen(error: String, onDismiss: () -> Unit) {
        MaterialTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DARK_BACKGROUND_COLOR),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .padding(start=COLUMN_PADDING,end=COLUMN_PADDING)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState(0)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(COLUMN_PADDING))
                    BasicText(
                        text = "ERROR",
                        style = TextStyle(color = Color.White, fontSize = TEXT_SIZE, textAlign = TextAlign.Center, textDecoration = TextDecoration.Underline, letterSpacing = 1.sp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally)
                            .padding(top = (SCREEN_HEIGHT * 0.04f).dp)
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    BasicText(
                        text = error,
                        style = textStyle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height((SCREEN_HEIGHT*0.05f).dp))
                    Button(
                        onClick = { onDismiss() },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .fillMaxHeight(0.4f)
                    ) {
                        BasicText(
                            text = "Dismiss",
                            style = textStyle
                        )
                    }
                    Spacer(modifier = Modifier.height((SCREEN_HEIGHT*0.05f).dp))
                }
            }
        }
    }

    @Preview(device = "id:wearos_large_round", apiLevel = 33)
    @Composable
    fun PreviewConnectingScreenBig() {
        initGlobalValues(454, 454)
        ConnectingScreen()
    }

    @Preview(device = "id:wearos_small_round", apiLevel = 33)
    @Composable
    fun PreviewConnectingScreenSmall() {
        initGlobalValues(384, 384)
        ConnectingScreen()
    }

    @Preview(device = "id:wearos_large_round", apiLevel = 33)
    @Composable
    fun PreviewPreviewConnectedNotInLobbyBig() {
        initGlobalValues(454, 454)
        ConnectedNotInLobbyScreen("123456")
    }

    @Preview(device = "id:wearos_small_round", apiLevel = 33)
    @Composable
    fun PreviewPreviewConnectedNotInLobbySmall() {
        initGlobalValues(384, 384)
        ConnectedNotInLobbyScreen("123456")
    }

    @Preview(device = "id:wearos_large_round", apiLevel = 33)
    @Composable
    fun PreviewConnectedNotInLobbyScreenBig() {
        initGlobalValues(454, 454)
        ConnectedNotInLobbyScreen("123456")
    }

    @Preview(device = "id:wearos_small_round", apiLevel = 33)
    @Composable
    fun PreviewConnectedNotInLobbyScreenSmall() {
        initGlobalValues(384, 384)
        ConnectedNotInLobbyScreen("123456")
    }

    @Preview(device = "id:wearos_large_round", apiLevel = 33)
    @Composable
    fun PreviewConnectedInLobbyScreenBig() {
        initGlobalValues(454, 454)
        ConnectedInLobbyScreen("123456", "ABC", "gameType", false) {}
    }

    @Preview(device = "id:wearos_small_round", apiLevel = 33)
    @Composable
    fun PreviewConnectedInLobbyScreenSmall() {
        initGlobalValues(384, 384)
        ConnectedInLobbyScreen("123456", "ABC", "gameType", false) {}
    }

    @Preview(device = "id:wearos_large_round", apiLevel = 33)
    @Composable
    fun PreviewErrorScreenBig() {
        initGlobalValues(454, 454)
        ErrorScreen("Error message") {}
    }

    @Preview(device = "id:wearos_small_round", apiLevel = 33)
    @Composable
    fun PreviewErrorScreenSmall() {
        initGlobalValues(384, 384)
        ErrorScreen("Error message") {}
    }

    @Preview(device = "id:wearos_large_round", apiLevel = 33)
    @Composable
    fun PreviewLongErrorScreenBig() {
        initGlobalValues(454, 454)
        val msg = "Error message\nwith new line character\nthat is long enough to cause the text to wrap around to the next line"
        ErrorScreen(msg) {}
    }

    @Preview(device = "id:wearos_small_round", apiLevel = 33)
    @Composable
    fun PreviewLongErrorScreenSmall() {
        initGlobalValues(384, 384)
        val msg = "Error message\nwith new line character\nthat is long enough to cause the text to wrap around to the next line"
        ErrorScreen(msg) {}
    }
}





