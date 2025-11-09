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
    private var isWarmup: Boolean? = null
    private var countdownTimer: Int? = null
    private var force: Boolean? = null
    private var experimentRunning: Boolean? = null
    private var experimentId: String? = null

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
    fun isWarmup(isWarmup: Boolean) = apply { this.isWarmup = isWarmup }
    fun countdownTimer(countdownTimer: Int) = apply { this.countdownTimer = countdownTimer }
    fun force(force: Boolean) = apply { this.force = force }
    fun experimentRunning(experimentRunning: Boolean) = apply { this.experimentRunning = experimentRunning }
    fun experimentId(experimentId: String) = apply { this.experimentId = experimentId }



    fun build() = GameRequest(type,playerId,lobbyId,gameType,success,message,duration,sessionId,sessionIds,data,timestamp,syncWindowLength,syncTolerance,isWarmup,countdownTimer,force,experimentRunning,experimentId)
}