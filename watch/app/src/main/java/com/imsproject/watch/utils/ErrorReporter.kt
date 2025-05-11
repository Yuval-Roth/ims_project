package com.imsproject.watch.utils

import android.util.Log
import com.imsproject.watch.model.MainModel
import com.imsproject.watch.model.REST_SCHEME
import com.imsproject.watch.model.SERVER_ERROR_REPORTS_PORT
import com.imsproject.watch.model.SERVER_IP
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object ErrorReporter {

    fun report(throwable: Throwable?, additionalInfo: String = "No additional info") {
        val logcatHistory = getLogcatHistory()
        val timestamp = LocalDateTime.now(ZoneId.of("Asia/Jerusalem")).timestamp()

        val errorReport = HTML_TEMPLATE
            .replace("{{timestamp}}", timestamp)
            .replace("{{additionalInfo}}", additionalInfo)
            .replace("{{errorMessage}}", throwable?.message ?: "No message available")
            .replace("{{stacktrace}}", throwable?.stackTraceToString() ?: "No stacktrace available")
            .replace("{{logcatHistory}}", logcatHistory)

        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.IO) {
            sendErrorReport(errorReport, timestamp)
        }
    }

    private fun sendErrorReport(htmlReport: String, timestamp: String) {
        val actor = MainModel.instance.playerId
        try {
            RestApiClient()
                .withUri("$REST_SCHEME://$SERVER_IP:$SERVER_ERROR_REPORTS_PORT/error-reports/add")
                .withBody(htmlReport)
                .withParam("timestamp", timestamp)
                .apply { actor?.let { withParam("actor", it) } }
                .withPost()
                .send()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send error report: ${e.message}")
        }
    }

    private fun getLogcatHistory(): String {
        val processId = android.os.Process.myPid().toString()
        val command = "logcat -d -v threadtime --pid $processId"
        val process = Runtime.getRuntime().exec(command.split(" ").toTypedArray())
        return String(process.inputStream.readAllBytes())
    }

    private fun LocalDateTime.timestamp(): String {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm:ss")
        return this.format(formatter)
    }

    private const val TAG = "ErrorReporter"

    private val HTML_TEMPLATE = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>Error Report</title>
          <style>
            body { 
              font-family: Arial, sans-serif; 
              line-height: 1.6; 
              color: #333; 
              background: #f9f9f9;
              margin: 0;
              padding: 0;
            }
            .container { 
              width: 80%; 
              margin: 20px auto; 
              padding: 20px; 
              border: 1px solid #ddd; 
              border-radius: 8px; 
              background: #fff;
            }
            h2 { color: #d9534f; }
            pre { 
              background: #eee; 
              padding: 10px; 
              border-radius: 5px; 
              overflow-x: auto; 
            }
          </style>
        </head>
        <body>
          <div class="container">
            <h2>Error Report</h2>
            <p><strong>Timestamp:</strong> {{timestamp}}</p>
            <h3>Error Message</h3>
            <pre>{{errorMessage}}</pre>
            <h3>Additional Info</h3>
            <pre>{{additionalInfo}}</pre>
            <h3>Stacktrace</h3>
            <pre>{{stacktrace}}</pre>
            <h3>Logcat History</h3>
            <pre>{{logcatHistory}}</pre>
          </div>
        </body>
        </html>
    """.trimIndent()
}
