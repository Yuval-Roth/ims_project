package com.imsproject.gameserver.api

import com.imsproject.common.gameserver.GameRequest
import com.imsproject.common.utils.Response
import com.imsproject.gameserver.dataAccess.DAOController
import com.imsproject.gameserver.toResponseEntity
import com.imsproject.common.utils.fromJson
import com.imsproject.gameserver.business.GameRequestFacade
import com.imsproject.gameserver.business.Participant
import com.imsproject.gameserver.business.ParticipantController
import com.imsproject.gameserver.business.auth.AuthController
import com.imsproject.gameserver.business.auth.Credentials
import org.slf4j.LoggerFactory
import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.core.io.ResourceLoader
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
class RestHandler(
    private val gameRequestFacade: GameRequestFacade,
    private val authController: AuthController,
    private val participantController: ParticipantController,
    private val daoController: DAOController,
    private val resources : ResourceLoader
    ) : ErrorController {

    @PostMapping("/manager")
    fun manager(@RequestBody body : String): ResponseEntity<String> {
        val request: GameRequest
        try{
            request = fromJson(body)
        } catch(e: Exception){
            return Response.getError("Error parsing request").toResponseEntity(HttpStatus.BAD_REQUEST)
        }

        try {
            return gameRequestFacade.handleGameRequest(request).toResponseEntity()
        } catch (e: Exception) {
            log.error("Error handling game request", e)
            return Response.getError(e).toResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @RequestMapping("/operators/{action}", method = [RequestMethod.POST, RequestMethod.GET])
    fun operators(
        @PathVariable action: String,
        @RequestBody body : String?
    ): ResponseEntity<String> {
        val credentials : Credentials = if(body != null){
            try{
                fromJson(body)
            } catch(e: Exception){
                return Response.getError("Error parsing request").toResponseEntity(HttpStatus.BAD_REQUEST)
            }
        } else {
            Credentials("","",null,null)
        }
        try{
            when(action){
                "add" -> authController.createUser(credentials)
                "remove" -> authController.deleteUser(credentials.userId)
                "get" -> {
                    val users = authController.getAllUsers()
                    return Response.getOk(users).toResponseEntity()
                }
                else -> Response.getError("Invalid action").toResponseEntity(HttpStatus.BAD_REQUEST)
            }
        } catch(e: IllegalArgumentException){
            return Response.getError(e).toResponseEntity(HttpStatus.BAD_REQUEST)
        } catch(e: Exception){
            return Response.getError(e).toResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
        return Response.getOk().toResponseEntity()
    }

    @RequestMapping("/participants/{action}", method = [RequestMethod.POST, RequestMethod.GET])
    fun participants(
        @PathVariable action: String,
        @RequestBody body : String?
    ): ResponseEntity<String> {
        val participant : Participant = if(body != null){
            try{
                fromJson(body)
            } catch(e: Exception){
                return Response.getError("Error parsing request").toResponseEntity(HttpStatus.BAD_REQUEST)
            }
        } else {
            Participant(null,null,null,null,null,null,null)
        }
        try{
            when(action){
                "add" -> {
                    val id = participantController.addParticipant(participant)
                    return Response.getOk(id).toResponseEntity()
                }
                "remove" -> participantController.remove(participant.pid!!)
                "get" -> {
                    val participants = participantController.getAll()
                    return Response.getOk(participants).toResponseEntity()
                }
                else -> Response.getError("Invalid action").toResponseEntity(HttpStatus.BAD_REQUEST)
            }
        } catch (e: IllegalArgumentException) {
            return Response.getError(e).toResponseEntity(HttpStatus.BAD_REQUEST)
        } catch (e: Exception) {
            return Response.getError(e).toResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
        return Response.getOk().toResponseEntity()
    }

    @PostMapping("/data/{section}/{action}")
    fun data(
            @PathVariable section: String,
            @PathVariable action: String,
            @RequestBody body: String,
        ): ResponseEntity<String> {

        try {
            return (daoController.handle(section, action, body)).toResponseEntity()
        } catch(e: Exception)
        {
            return Response.getError(e).toResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
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

    companion object {
        private val log = LoggerFactory.getLogger(RestHandler::class.java)
    }
}