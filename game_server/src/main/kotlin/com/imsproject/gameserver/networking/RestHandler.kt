package com.imsproject.gameserver.networking

import com.imsproject.common.gameServer.GameRequest
import com.imsproject.common.utils.JsonUtils
import com.imsproject.common.utils.Response
import com.imsproject.gameserver.GameController
import com.imsproject.gameserver.auth.AuthController
import com.imsproject.gameserver.auth.Credentials
import com.imsproject.gameserver.toResponseEntity
import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.core.io.ResourceLoader
import org.springframework.http.HttpStatus
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

    @PostMapping("/operators/{action}")
    fun operators(
        @PathVariable action: String,
        @RequestBody body : String
    ): ResponseEntity<String> {
        val credentials : Credentials = JsonUtils.deserialize(body)
        try{
            when(action){
                "add" -> authController.createUser(credentials)
                "remove" -> authController.deleteUser(credentials.userId)
                else -> Response.getError("Invalid action").toResponseEntity(HttpStatus.BAD_REQUEST)
            }
        } catch(e: IllegalArgumentException){
            return Response.getError(e).toResponseEntity(HttpStatus.BAD_REQUEST)
        } catch(e: Exception){
            return Response.getError(e).toResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
        return Response.getOk().toResponseEntity()
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
        val decoded = String(Base64.getDecoder().decode(credentials))
        val split = decoded.split(":")
        val userId = split[0]
        val password = split[1]
        return authController.authenticateUser(userId, password).toResponseEntity()
    }

    @GetMapping("/bcrypt")
    fun bcrypt(): ResponseEntity<String> {
        return readHtmlFile("static/bcrypt.html").toResponseEntity()
    }

    @PostMapping("/bcrypt/encrypt")
    fun bcryptSend(@RequestBody password : String): ResponseEntity<String> {
        return authController.textToBCrypt(password).toResponseEntity()
    }

    @GetMapping("/error")
    fun errorPage(): ResponseEntity<String> {
        val code = HttpStatus.BAD_REQUEST
        return readHtmlFile("static/error_page.html")
            .replace("[MESSAGE]", "Something went wrong")
            .replace("[TIME_STAMP]", Date().toString())
            .replace("[STATUS]", code.toString())
            .toResponseEntity(code)
    }

    private fun readHtmlFile(path: String): String {
        return resources.getResource("classpath:$path").inputStream
            .bufferedReader().use { it.readText() }
    }
}