package com.imsproject.watch.view

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentSanitizer
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import com.imsproject.common.gameserver.GameType
import com.imsproject.watch.DARK_BACKGROUND_COLOR
import com.imsproject.watch.PACKAGE_PREFIX
import com.imsproject.watch.SCREEN_WIDTH
import com.imsproject.watch.textStyle
import com.imsproject.watch.utils.ErrorReporter
import com.imsproject.watch.view.contracts.Result
import com.imsproject.watch.viewmodel.GameViewModel

abstract class GameActivity(gameType: GameType) : ComponentActivity() {

    private val TAG = "$_TAG-${gameType.prettyName()}"

    protected fun setupUncaughtExceptionHandler(viewModel: GameViewModel) {
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            ErrorReporter.report(e)
            viewModel.exitWithError("""
                Uncaught Exception in thread ${t.name}:
                ${e.stackTraceToString()}
                """.trimIndent(), Result.Code.UNKNOWN_ERROR
            )
        }
    }

    @Composable
    protected fun Main(viewModel: GameViewModel){
        val state by viewModel.state.collectAsState()
        when(state){
            GameViewModel.State.LOADING -> {
                LoadingScreen("Loading session...")
            }
            GameViewModel.State.TRYING_TO_RECONNECT -> {
                LoadingScreen("Trying to reconnect...")
            }
            GameViewModel.State.ERROR -> {
                val error = viewModel.error.collectAsState().value ?: "Unknown error"
                ErrorScreen(error) {
                    viewModel.clearError()
                }
            }
            GameViewModel.State.TERMINATED -> {
                BlankScreen()
                val result = viewModel.resultCode.collectAsState().value
                val intent = IntentSanitizer.Builder()
                    .allowComponent(componentName)
                    .build().sanitize(intent) {
                        Log.d(TAG, it)
                    }
                if(result != Result.Code.OK){
                    intent.putExtra("$PACKAGE_PREFIX.error", viewModel.error.collectAsState().value)
                }
                setResult(result.ordinal,intent)
                finish()
            }
            else -> {
                Log.e(TAG, "unexpected state $state")
                finish()
            }
        }
    }

    @Composable
    private fun LoadingScreen(text: String) {
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
                        modifier = Modifier.size((SCREEN_WIDTH *0.2f).dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    BasicText(
                        text = text,
                        style = textStyle
                    )
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

    companion object {
        private const val _TAG = "GameActivity"
    }
}