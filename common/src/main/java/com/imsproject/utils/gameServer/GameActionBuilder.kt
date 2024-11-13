package com.imsproject.utils.gameServer

class GameActionBuilder internal constructor (private val type: GameAction.Type){

    var data: String? = null

    fun data(data: String?) = apply { this.data = data }

    fun build() = GameAction(type, data)
}