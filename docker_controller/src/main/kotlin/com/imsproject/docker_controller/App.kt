package com.imsproject.docker_controller

import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.core.io.ResourceLoader
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
class App (private val resources : ResourceLoader) : ErrorController {
    @GetMapping("/")
    fun home(): ResponseEntity<String> {
        return readHtmlFile("static/home.html").toResponseEntity()
    }

    @GetMapping("/docker/update")
    fun updateDocker(): ResponseEntity<String> {
        try{
            ProcessBuilder("sh", "/home/admin/pull_and_restart").start()
        } catch(e: Exception){
            val error = "Error updating docker<br/>${e.message}"
            return getErrorPage(error, HttpStatus.INTERNAL_SERVER_ERROR)
        }
        return readHtmlFile("static/success.html").toResponseEntity()
    }

    @GetMapping("/error")
    fun errorPage(): ResponseEntity<String> {
        return getErrorPage("Something went wrong", HttpStatus.BAD_REQUEST)
    }

    private fun getErrorPage(message: String, code: HttpStatusCode): ResponseEntity<String> {
        return readHtmlFile("static/error_page.html")
            .replace("[MESSAGE]", message)
            .replace("[TIME_STAMP]", Date().toString())
            .replace("[STATUS]", code.toString())
            .toResponseEntity(code)
    }

    private fun String.toResponseEntity (errorCode: HttpStatusCode): ResponseEntity<String> {
        return ResponseEntity.status(errorCode).body(this)
    }

    private fun String.toResponseEntity (): ResponseEntity<String> {
        return ResponseEntity.ok(this)
    }

    private fun readHtmlFile(path: String): String {
        return resources.getResource("classpath:$path").inputStream
            .bufferedReader().use { it.readText() }
    }
}