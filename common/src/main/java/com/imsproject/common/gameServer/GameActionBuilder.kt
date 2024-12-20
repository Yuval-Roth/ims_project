package com.imsproject.common.gameServer

class GameActionBuilder internal constructor (private val type: GameAction.Type){

    private var actor: String? = null
    private var data: String? = null
    private var timestamp: String? = null

    fun actor(actor: String?) = apply { this.actor = actor }
    fun data(data: String?) = apply { this.data = data }
    fun timestamp(timeStamp: String?) = apply { this.timestamp = timeStamp }

    fun build() = GameAction(type,actor, data, timestamp)
}