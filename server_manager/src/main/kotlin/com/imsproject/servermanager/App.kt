package com.imsproject.servermanager

import com.imsproject.common.utils.Response
import com.imsproject.common.utils.fromJson
import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import java.io.File
import java.net.URLEncoder
import java.util.*

const val ONE_DAY_MILLIS = 24 * 60 * 60 * 1000

fun String.toResponseEntity (errorCode: HttpStatusCode): ResponseEntity<String> {
    return ResponseEntity.status(errorCode).body(this)
}

fun String.toResponseEntity (): ResponseEntity<String> {
    return ResponseEntity.ok(this)
}

fun String.htmlNewLines(): String {
    return this.replace("\n", "<br/>")
}

fun redirectToError(msg: String): ResponseEntity<String> {
    val encodedMsg = URLEncoder.encode(msg,"UTF-8")
    return ResponseEntity
        .status(HttpStatus.FOUND)
        .header("Location", "/error?msg=$encodedMsg")
        .build()
}

const val errorReportsFolderPath = "/home/admin/error_reports"

@Controller
class App : ErrorController {

    init {
        File(errorReportsFolderPath).mkdirs()
    }

    @GetMapping("/")
    fun home(model: Model): String {
        try {
            val processes = getDockerProcesses()
            model.addAttribute("processes", processes)
            model.addAttribute("error", null)
        } catch (e: Exception) {
            model.addAttribute("processes", emptyList<DockerProcess>())
            model.addAttribute("error", "Error fetching docker processes: ${e.message}")
        }
        return "home"
    }

    @GetMapping("/docker/update")
    fun dockerUpdate(model: Model): String {
        return runCommand(
            "sh /home/admin/pull_and_restart",
            "Docker update initiated successfully",
            "Error updating docker"
        )
    }

    @GetMapping("/docker/up")
    fun dockerUp(model: Model): String {
        return runCommand(
            "docker compose -f /home/admin/compose.yml up --detach",
            "Docker services started successfully",
            "Error starting docker services"
        )
    }

    @GetMapping("/docker/down")
    fun dockerDown(model: Model): String {
        return runCommand(
            "docker compose -f /home/admin/compose.yml down",
            "Docker services shutdown initiated successfully",
            "Error stopping docker services"
        )
    }

    @GetMapping("/docker/restart")
    fun dockerRestart(model: Model): String {
        return runCommand(
            "docker compose -f /home/admin/compose.yml restart",
            "Docker services restarted successfully",
            "Error restarting docker services"
        )
    }

    @GetMapping("/error-reports")
    fun errorReportsPage(
        model: Model,
        @RequestParam(value = "notolderthan", required = false, defaultValue = "$ONE_DAY_MILLIS") notOlderThan: Long
    ): String {
        val reportsDir = File(errorReportsFolderPath)
        if (!reportsDir.exists() || !reportsDir.isDirectory) {
            return error("Error reports directory not found")
        }

        val now = System.currentTimeMillis()
        val files = reportsDir.listFiles()
            ?.filter { it.isFile && if(notOlderThan > 0) now - it.lastModified() <= notOlderThan else true }
            ?.sortedByDescending { it.lastModified() }  // Sort by last modified timestamp
            ?.map { it.name }  // Extract just the filenames after sorting
            ?: emptyList()

        model.addAttribute("reports", files)
        return "error-reports"
    }

    @GetMapping("/success")
    fun successPage(model: Model, @RequestParam(value = "msg") msg : String): String {
        model.addAttribute("message", msg)
        return "success"
    }

    @GetMapping("/error")
    fun errorPage(
        model: Model,
        @RequestParam("msg", required = false, defaultValue = "Something went wrong") msg : String
    ): String {
        model.addAttribute("message", msg)
        model.addAttribute("timestamp", Date().toString())
        model.addAttribute("status", HttpStatus.INTERNAL_SERVER_ERROR)
        return "error"
    }

    private fun getDockerProcesses(): List<DockerProcess> {
        val proc = ProcessBuilder("docker ps --format json".split(" ")).start()
        proc.waitFor()

        val error = proc.errorStream.bufferedReader().use { it.readText() }
        if (error.isNotEmpty()) throw RuntimeException(error)

        val output = proc.inputStream.bufferedReader().use { it.readText() }
        return output.split("\n")
            .filter { it.isNotBlank() }
            .map { fromJson(it) }
    }

    private fun runCommand(command: String, successMessage: String, errorMessage: String): String {
        val commandParts = command.split(" ")
        try{
            ProcessBuilder(commandParts).start()
        } catch(e: Exception){
            return error("$errorMessage: ${e.message}")
        }
        return success(successMessage)
    }

    private fun error(msg: String): String {
        val urlMsg = URLEncoder.encode(msg,"UTF-8")
        return "redirect:/error?msg=$urlMsg"
    }

    private fun success(msg: String): String {
        val urlMsg = URLEncoder.encode(msg,"UTF-8")
        return "redirect:/success?msg=$urlMsg"
    }
}

@RestController
class LogsProvider {

    @GetMapping("/logs/game-server")
    fun gameServerLog(
        @RequestParam(value = "rows", required = false, defaultValue = "-1") rows: Int
    ): ResponseEntity<String> {
        return readLog("/home/admin/volumes/game_server/logs/application.log", rows)
    }

    @GetMapping("/logs/manager")
    fun managerLog(
        @RequestParam(value = "rows", required = false, defaultValue = "-1") rows: Int
    ): ResponseEntity<String> {
        return readLog("/home/admin/volumes/manager/logs/output.log", rows)
    }

    private fun readLog(path: String, rows:Int): ResponseEntity<String> {
        try {
            val logFile = File(path)
            var file = logFile.readText()
            file = if (rows > 0) {
                val split = file.split("\n")
                split.takeLast(rows).joinToString("<br/>")
            } else {
                file.htmlNewLines()
            }
            file += """
                <script>
                    window.onload = function() {
                        setTimeout(function() {
                            window.scrollTo(0, document.body.scrollHeight);
                        }, 10);
                    }
                </script>
              """.trimIndent()
            return file.toResponseEntity()
        } catch (e: Exception) {
            val msg = "Error while fetching log file:\n${
                e.message ?: e.stackTraceToString().substring(0,1950)
            }"
            return redirectToError(msg)
        }
    }
}

@RestController
class ErrorReceiver {

    @GetMapping("/error-reports/get")
    fun getErrorReport(
        @RequestParam("filename") fileName: String,
    ): ResponseEntity<String> {
        try {
            val logFile = File("$errorReportsFolderPath/$fileName")
            return logFile.readText().toResponseEntity()
        } catch (e: Exception) {
            val errorMsg = "Error while fetching error report:\n${
                e.message ?: e.stackTraceToString().substring(0,1950)
            }"
            return redirectToError(errorMsg)
        }
    }

    @PostMapping("/error-reports/add")
    fun addErrorReport(
        @RequestParam("timestamp") timestamp: String,
        @RequestParam(value = "actor", required = false, defaultValue = "unspecified-actor") actor: String,
        @RequestBody report: String
    ) : ResponseEntity<String> {
        val escapedTimestamp = timestamp
            .replace(" ", "")
            .replace("/", ".")
            .replace("-","@")
            .replace(":", "-")
        var filePath = "$errorReportsFolderPath/$escapedTimestamp@$actor"
        var index = 1
        try {
            while(File(filePath).exists()){
                filePath = "$errorReportsFolderPath/$escapedTimestamp-${actor}_${index++}"
            }
            File(filePath).writeText(report)
            return Response.getOk().toResponseEntity()
        } catch (e: Exception) {
            val errorMsg = "Error while saving error report:<br/>${
                e.stackTraceToString().htmlNewLines()
            }"
            return Response.getError(errorMsg).toResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    data class FileName(val fileName: String)
    @PostMapping("/error-reports/delete")
    fun deleteErrorReport(
        @RequestBody body: String
    ): ResponseEntity<String> {
        try {
            val fileName = fromJson<FileName>(body).fileName
            val logFile = File("$errorReportsFolderPath/$fileName")
            logFile.delete()
            return Response.getOk().toResponseEntity()
        } catch (e: Exception) {
            val errorMsg = "Error while deleting error report:\n${
                e.message ?: e.stackTraceToString().substring(0,1950)
            }"
            return Response.getError(errorMsg).toResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }
}