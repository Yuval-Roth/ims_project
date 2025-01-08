package com.imsproject.common.gameserver

class GameActionBuilder internal constructor (private val type: GameAction.Type){

    private var actor: String? = null
    private var data: String? = null
    private var timestamp: String? = null
    private var sequenceNumber: Long? = null

    fun actor(actor: String?) = apply { this.actor = actor }
    fun data(data: String?) = apply { this.data = data }
    fun timestamp(timeStamp: String?) = apply { this.timestamp = timeStamp }
    fun sequenceNumber(sequenceNumber: Long?) = apply { this.sequenceNumber = sequenceNumber }

    fun build() = GameAction(type,actor, data, timestamp, sequenceNumber)
}