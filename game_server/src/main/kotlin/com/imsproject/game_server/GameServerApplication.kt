package com.imsproject.game_server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class GameServerApplication

fun main(args: Array<String>) {
	runApplication<GameServerApplication>(*args)
}
