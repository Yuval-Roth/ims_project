package com.imsproject.gameServer.networking

import com.imsproject.common.gameServer.GameRequest
import com.imsproject.common.utils.Response
import com.imsproject.gameServer.GameController
import com.imsproject.gameServer.auth.AuthController
import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.core.io.ResourceLoader
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.File
import java.util.*

@RestController
class RestHandler(
    private val gameController: GameController,
    private val authController: AuthController,
    private val resources : ResourceLoader
    ) : ErrorController {

    @PostMapping("/manager")
    fun manager(@RequestBody body : String): ResponseEntity<String> {
        val request: GameRequest?
        try{
            request = GameRequest.fromJson(body)
        } catch(e: Exception){
            return Response.getError("Error parsing request").toResponseEntity(HttpStatus.BAD_REQUEST)
        }
        return try{
            gameController.handleGameRequest(request).toResponseEntity()
        } catch(e: Exception){
            Response.getError(e).toResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @PostMapping("/data")
    fun data(@RequestBody body : String): ResponseEntity<String> {
        return "Not implemented".toResponseEntity()
    }

    @GetMapping("/login")
    fun login(@RequestHeader(value = "Authorization") header : String?): ResponseEntity<String> {
        if(header == null){
            return Response.getError("No Authorization header").toResponseEntity(HttpStatus.BAD_REQUEST)
        }
        // base64 encoded user:password
        val credentials = header.split(" ")[1]
        val decoded = String(java.util.Base64.getDecoder().decode(credentials))
        val split = decoded.split(":")
        val userId = split[0]
        val password = split[1]
        return authController.authenticateUser(userId, password).toJson().toResponseEntity()
    }

    @GetMapping("/log")
    fun log(
        @RequestParam(value = "rows", required = false, defaultValue = "-1") rows : Int
    ): ResponseEntity<String> {
        try{
            val logFile = File("/app/data/logs/application.log")
            val file = logFile.readText()
            return if(rows > 0) {
                val split = file.split("\n")
                split.takeLast(rows).joinToString("<br/>").toResponseEntity()
            } else {
                file.replace("\n", "<br/>").toResponseEntity()
            }
        } catch(e: Exception){
            return "Error while fetching log file:<br/>${e.stackTraceToString()
                .replace("\n", "<br/>")}"
                .toResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @GetMapping("/bcrypt")
    fun bcrypt(): ResponseEntity<String> {
        return readHtmlFile("bcrypt.html").toResponseEntity()
    }

    @PostMapping("/bcrypt/encrypt")
    fun bcryptSend(@RequestBody password : String): ResponseEntity<String> {
        return authController.textToBCrypt(password).toResponseEntity()
    }

    @GetMapping("/error")
    fun errorPage(): ResponseEntity<String> {
        val code = HttpStatus.BAD_REQUEST
        return readHtmlFile("error_page.html")
            .replace("[MESSAGE]", "Something went wrong")
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