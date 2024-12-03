package com.imsproject.gameServer.networking

import com.imsproject.common.gameServer.GameRequest
import com.imsproject.common.utils.Response
import com.imsproject.gameServer.GameController
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class RestHandler(private val gameController: GameController) {

    @PostMapping("/manager")
    fun manager(@RequestBody body : String): String {
        val request: GameRequest?
        try{
            request = GameRequest.fromJson(body)
        } catch(e: Exception){
            return Response.getError(e)
        }
        return gameController.handleGameRequest(request)
    }

    @PostMapping("/data")
    fun data(@RequestBody body : String): String {
        return "Not implemented"
    }
}