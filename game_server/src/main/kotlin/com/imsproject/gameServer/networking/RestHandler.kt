package com.imsproject.gameServer.networking

import com.imsproject.gameServer.GameController
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class RestHandler(private val gameController: GameController) {

    @GetMapping("/manager/get_all_lobbies")
    fun getLobbies(): String {
        return gameController.getAllLobbies()
    }

    @GetMapping("/manager/get_lobby")
    fun getLobby(@RequestParam(value = "lobbyid", required = true) lobbyId : String): String {
        return gameController.getLobby(lobbyId)
    }

    @GetMapping("/manager/leave_lobby")
    fun leaveLobby(@RequestParam(value = "lobbyid", required = true) lobbyId : String,
                     @RequestParam(value = "playerid", required = true) playerId : String): String {
        return gameController.leaveLobby(lobbyId,playerId)
    }

    @GetMapping("/manager/join_lobby")
    fun joinLobby(@RequestParam(value = "lobbyid", required = true) lobbyId : String,
                     @RequestParam(value = "playerid", required = true) playerId : String): String {
        return gameController.joinLobby(lobbyId,playerId)
    }

    @GetMapping("/manager/create_lobby")
    fun createLobby(@RequestParam (value = "gametype", required = true) gameType : String): String {
        return gameController.createLobby(gameType)
    }

    @GetMapping("/manager/start_game")
    fun startGame(@RequestParam(value = "lobbyid", required = true) lobbyId : String): String {
        return gameController.startGame(lobbyId)
    }

    @GetMapping("/manager/end_game")
    fun endGame(@RequestParam(value = "lobbyid", required = true) lobbyId : String): String {
        return gameController.endGame(lobbyId)
    }

    @GetMapping("/manager/set_lobby_type")
    fun setLobbyType(@RequestParam(value = "lobbyid", required = true) lobbyId : String,
                     @RequestParam(value = "gametype", required = true) gameType : String): String {
        return gameController.setLobbyType(lobbyId,gameType)
    }
}