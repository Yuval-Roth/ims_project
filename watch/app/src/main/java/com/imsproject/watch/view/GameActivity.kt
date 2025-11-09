package com.imsproject.watch.view

import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.IntentSanitizer
import com.imsproject.common.gameserver.GameType
import com.imsproject.watch.PACKAGE_PREFIX
import com.imsproject.watch.initProperties
import com.imsproject.watch.utils.ErrorReporter
import com.imsproject.watch.view.contracts.Result
import com.imsproject.watch.viewmodel.GameViewModel

abstract class GameActivity(gameType: GameType) : ComponentActivity() {

    private val TAG = "$_TAG-${gameType.prettyName()}"
    private lateinit var viewModel: GameViewModel

    protected fun onCreate(viewModel: GameViewModel) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val metrics = getSystemService(WindowManager::class.java).currentWindowMetrics
        viewModel.screenDensity = resources.displayMetrics.density
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
                LoadingScreen("טוען...")
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
                intent.putExtra("$PACKAGE_PREFIX.error", viewModel.error.collectAsState().value)
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
    fun CheckConnection() {
        val reconnecting by viewModel.reconnecting.collectAsState()
        if(reconnecting) {
            ReconnectingOverlay {
                Log.e(TAG, "ReconnectingOverlay: timed out, exiting game")
                viewModel.exitWithError("Disconnected from server", Result.Code.CONNECTION_LOST)
            }
        }
    }

    companion object {
        private const val _TAG = "GameActivity"
    }
}