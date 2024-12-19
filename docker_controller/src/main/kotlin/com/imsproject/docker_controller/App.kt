package com.imsproject.docker_controller

import com.google.gson.Gson
import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.core.io.ResourceLoader
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import java.net.URLEncoder
import java.util.*

@Controller
class App (private val resources : ResourceLoader) : ErrorController {

    @GetMapping("/")
    fun home(model: Model) : String {
        val proc : Process
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
    fun dockerUp(model: Model): String{
        return runCommand(
            "docker compose -f /home/admin/compose.yml up --detach",
            "Docker services started successfully",
            "Error starting docker services"
        )
    }

    @GetMapping("/docker/down")
    fun dockerDown(model: Model): String{
        return runCommand(
            "docker compose -f /home/admin/compose.yml down",
            "Docker services shutdown initiated successfully",
            "Error stopping docker services"
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