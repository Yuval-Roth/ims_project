package com.imsproject.gameServer.networking

import com.imsproject.common.gameServer.GameRequest
import com.imsproject.common.utils.Response
import com.imsproject.gameServer.GameController
import org.springframework.web.bind.annotation.*
import java.io.File

@RestController
class RestHandler(private val gameController: GameController) {

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

    @GetMapping("/auth")
    fun auth(): String {
        return "Not implemented"
    }

    @PostMapping("/data")
    fun data(@RequestBody body : String): String {
        return "Not implemented"
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
            return "404"
        }
    }
}