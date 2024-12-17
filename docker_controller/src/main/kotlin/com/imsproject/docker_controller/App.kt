package com.imsproject.docker_controller

import com.google.gson.Gson
import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.core.io.ResourceLoader
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import java.util.*

@Controller
class App (private val resources : ResourceLoader) : ErrorController {

    @GetMapping("/")
    fun home(model: Model) : String {
        val proc : Process
        try {
            proc = ProcessBuilder("docker", "ps","--format","json").start()
        } catch (e: Exception) {
            val error = "Error fetching docker processes: ${e.message}"
            model.addAttribute("message", error)
            model.addAttribute("timestamp", Date().toString())
            model.addAttribute("status", HttpStatus.INTERNAL_SERVER_ERROR)
            return "error"
        }
        // get standard output
        val output = proc.inputStream.bufferedReader().use { it.readText() }
        // get error output
        val error = proc.errorStream.bufferedReader().use { it.readText() }
        if (error.isNotEmpty()) {
            val msg = "Error fetching docker processes: $error"
            model.addAttribute("message", msg)
            model.addAttribute("timestamp", Date().toString())
            model.addAttribute("status", HttpStatus.INTERNAL_SERVER_ERROR)
            return "error"
        }
        val gson = Gson()
        val processes = output
            .split("\n")
            .mapNotNull { gson.fromJson(it, DockerProcess::class.java) }
        model.addAttribute("processes", processes)

        return "home"
    }

    @GetMapping("/docker/update")
    fun updateDocker(model: Model): String {
        try{
            ProcessBuilder("sh", "/home/admin/pull_and_restart").start()
        } catch(e: Exception){
            val msg = "Error updating docker: ${e.message}"
            model.addAttribute("message", msg)
            model.addAttribute("timestamp", Date().toString())
            model.addAttribute("status", HttpStatus.INTERNAL_SERVER_ERROR)
            return "error"
        }
        return "success"
    }

    @GetMapping("/error")
    fun errorPage(model: Model): String {
        model.addAttribute("message", "Something went wrong")
        model.addAttribute("timestamp", Date().toString())
        model.addAttribute("status", HttpStatus.INTERNAL_SERVER_ERROR)
        return "error"
    }
}