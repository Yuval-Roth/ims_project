package com.imsproject.gameServer.networking

import com.imsproject.common.gameServer.GameRequest
import com.imsproject.common.utils.Response
import com.imsproject.gameServer.GameController
import com.imsproject.gameServer.auth.AuthController
import org.springframework.core.io.ResourceLoader
import org.springframework.web.bind.annotation.*
import java.io.File

@RestController
class RestHandler(
    private val gameController: GameController,
    private val authController: AuthController,
    private val resources : ResourceLoader
    ) {

    @PostMapping("/manager")
    fun manager(@RequestBody body : String): String {
        val request: GameRequest?
        try{
            request = GameRequest.fromJson(body)
        } catch(e: Exception){
            return Response.getError("Error parsing request")
        }
        return try{
            gameController.handleGameRequest(request)
        } catch(e: Exception){
            Response.getError(e)
        }
    }

    @PostMapping("/data")
    fun data(@RequestBody body : String): String {
        return "Not implemented"
    }

    @GetMapping("/login")
    fun login(@RequestHeader(value = "Authorization") header : String?): String {
        if(header == null){
            return Response.getError("No Authorization header")
        }
        // base64 encoded user:password
        val credentials = header.split(" ")[1]
        val decoded = String(java.util.Base64.getDecoder().decode(credentials))
        val split = decoded.split(":")
        val userId = split[0]
        val password = split[1]
        return authController.authenticateUser(userId, password).toJson()
    }

    @GetMapping("/log")
    fun log(@RequestParam(value = "rows", required = false, defaultValue = "-1") rows : Int): String {
        try{
            val logFile = File("/app/data/logs/application.log")
            val file = logFile.readText()
            return if(rows > 0) {
                val split = file.split("\n")
                split.takeLast(rows).joinToString("<br/>")
            } else {
                file.replace("\n", "<br/>")
            }
        } catch(e: Exception){
            return "Error while fetching log file:<br/>${e.stackTraceToString().replace("\n", "<br/>")}"
        }
    }

    @GetMapping("/bcrypt")
    fun bcrypt(): String {
        return resources.getResource("classpath:bcrypt.html").inputStream.bufferedReader().use { it.readText() }
    }

    @PostMapping("/bcrypt/encrypt")
    fun bcryptSend(@RequestBody password : String): String {
        return authController.textToBCrypt(password)
    }
}