package com.imsproject.gameserver

class ManagerEventBuilder internal constructor(val type: ManagerEvent.Type) {

    private var playerId: String? = null
    private var lobbyId: String? = null

    fun playerId(playerId: String) = apply { this.playerId = playerId }
    fun lobbyId(lobbyId: String) = apply { this.lobbyId = lobbyId }

    fun build() = ManagerEvent(type, playerId, lobbyId)
}
