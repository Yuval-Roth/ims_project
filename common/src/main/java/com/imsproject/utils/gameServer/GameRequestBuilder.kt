package com.imsproject.utils.gameServer

class GameRequestBuilder internal constructor(private val type: GameRequest.Type){

    private var playerId: String? = null
    private var lobbyId : String? = null
    private var gameType : GameType? = null
    private var port : Int? = null
    private var success : Boolean? = null
    private var message : String? = null
    private var data : List<String> ? = null

    fun playerId(playerId: String) = apply { this.playerId = playerId }
    fun lobbyId(lobbyId: String) = apply { this.lobbyId = lobbyId }
    fun gameType(gameType: GameType) = apply { this.gameType = gameType }
    fun port(port: Int) = apply { this.port = port }
    fun success(success: Boolean) = apply { this.success = success }
    fun message(message: String) = apply { this.message = message }
    fun data(data: List<String>) = apply { this.data = data }

    fun build() = GameRequest(type, playerId, lobbyId, gameType, port,success,message,data)
}