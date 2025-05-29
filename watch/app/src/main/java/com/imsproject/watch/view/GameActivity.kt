package com.imsproject.watch.view

import android.util.Log
import android.view.WindowManager
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
import com.imsproject.watch.SCREEN_RADIUS
import com.imsproject.watch.initProperties
import com.imsproject.watch.textStyle
import com.imsproject.watch.utils.ErrorReporter
import com.imsproject.watch.view.contracts.Result
import com.imsproject.watch.viewmodel.GameViewModel

abstract class GameActivity(gameType: GameType) : ComponentActivity() {

    private val TAG = "$_TAG-${gameType.prettyName()}"
    private lateinit var viewModel: GameViewModel

    protected fun onCreate(viewModel: GameViewModel) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val metrics = getSystemService(WindowManager::class.java).currentWindowMetrics
        initProperties(metrics.bounds.width())
        viewModel.onCreate(intent,applicationContext)
        this.viewModel = viewModel
        setupUncaughtExceptionHandler()
    }

    private fun setupUncaughtExceptionHandler() {
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
    protected open fun Main(){
        val state by viewModel.state.collectAsState()
        when(state){
            GameViewModel.State.LOADING -> {
                LoadingScreen("טוען פעילות...")
            }
            GameViewModel.State.TRYING_TO_RECONNECT -> {
                LoadingScreen("מנסה להתחבר מחדש....")
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
                if (result.name.substring(0,2) == "OK") {
                    intent.putExtra("$PACKAGE_PREFIX.expId", viewModel.expId.collectAsState().value)
                } else {
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
                        strokeWidth = (SCREEN_RADIUS * 0.04f).dp,
                        modifier = Modifier.size((SCREEN_RADIUS *0.4f).dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    RTLText(
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