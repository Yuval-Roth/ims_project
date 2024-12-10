package com.imsproject.common.gameServer

class GameActionBuilder internal constructor (private val type: GameAction.Type){

    var actor: String? = null
    var data: String? = null
    var inSync: Boolean? = null

    fun actor(actor: String?) = apply { this.actor = actor }
    fun data(data: String?) = apply { this.data = data }
    fun inSync(inSync: Boolean?) = apply { this.inSync = inSync }

    fun build() = GameAction(type,actor, data, inSync)
}