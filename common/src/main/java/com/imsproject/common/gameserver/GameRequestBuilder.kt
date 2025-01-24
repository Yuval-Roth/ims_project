package com.imsproject.common.gameserver

class GameRequestBuilder internal constructor(private val type: GameRequest.Type){

    private var playerId: String? = null
    private var lobbyId : String? = null
    private var gameType : GameType? = null
    private var success : Boolean? = null
    private var message : String? = null
    private var duration : Int? = null
    private var sessionId : String? = null
    private var sessionIds : List<String>? = null
    private var data : List<String> ? = null
    private var timestamp : String? = null
    private var syncWindowLength: Long? = null
    private var syncTolerance: Long? = null

    fun playerId(playerId: String) = apply { this.playerId = playerId }
    fun lobbyId(lobbyId: String) = apply { this.lobbyId = lobbyId }
    fun gameType(gameType: GameType) = apply { this.gameType = gameType }
    fun success(success: Boolean) = apply { this.success = success }
    fun message(message: String) = apply { this.message = message }
    fun duration(duration: Int) = apply { this.duration = duration }
    fun sessionId(sessionId: String) = apply { this.sessionId = sessionId }
    fun sessionIds(sessionIds: List<String>) = apply { this.sessionIds = sessionIds }
    fun data(data: List<String>) = apply { this.data = data }
    fun timestamp(timeStamp: String) = apply { this.timestamp = timeStamp }
    fun syncWindowLength(syncWindowLength: Long) = apply { this.syncWindowLength = syncWindowLength }
    fun syncTolerance(syncTolerance: Long) = apply { this.syncTolerance = syncTolerance }


    fun build() = GameRequest(type,playerId,lobbyId,gameType,success,message,duration,sessionId,sessionIds,data,timestamp,syncWindowLength,syncTolerance)
}