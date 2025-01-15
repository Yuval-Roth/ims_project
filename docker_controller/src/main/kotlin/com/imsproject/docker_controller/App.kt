package com.imsproject.docker_controller

import com.google.gson.Gson
import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.io.File
import java.net.URLEncoder
import java.util.*

@Controller
class App : ErrorController {

    @GetMapping("/")
    fun home(model: Model): String {
        val proc: Process
        try {
            proc = ProcessBuilder("docker ps --format json".split(" ")).start()
        } catch (e: Exception) {
            val error = "Error fetching docker processes: ${e.message}"
            return error(error)
        }
        proc.waitFor()
        // get standard output
        val output = proc.inputStream.bufferedReader().use { it.readText() }
        // get error output
        val error = proc.errorStream.bufferedReader().use { it.readText() }
        if (error.isNotEmpty()) {
            val msg = "Error fetching docker processes: $error"
            return error(msg)
        }
        val gson = Gson()
        val processes = output
            .split("\n")
            .mapNotNull { gson.fromJson(it, DockerProcess::class.java) }
        model.addAttribute("processes", processes)

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

    fun readLog(path: String, rows:Int): ResponseEntity<String> {
        try {
            val logFile = File(path)
            var file = logFile.readText()
            file = if (rows > 0) {
                val split = file.split("\n")
                split.takeLast(rows).joinToString("<br/>")
            } else {
                file.replace("\n", "<br/>")
            }
            file += """
                <script>
                    window.scrollTo(0, document.body.scrollHeight);
                </script>
              """.trimIndent()
            return file.toResponseEntity()
        } catch (e: Exception) {
            return "Error while fetching log file:<br/>${
                e.stackTraceToString()
                    .replace("\n", "<br/>")
            }".toResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }


    private fun String.toResponseEntity (errorCode: HttpStatusCode): ResponseEntity<String> {
        return ResponseEntity.status(errorCode).body(this)
    }

    private fun String.toResponseEntity (): ResponseEntity<String> {
        return ResponseEntity.ok(this)
    }
}