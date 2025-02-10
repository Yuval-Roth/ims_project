package com.imsproject.gameserver

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.event.EventListener

@SpringBootApplication
class GameServerApplication {
	@EventListener(ApplicationReadyEvent::class)
	fun setUncaughtExceptionHandler(event: ApplicationReadyEvent) {
		Thread.setDefaultUncaughtExceptionHandler { t, e ->
			log.error("Uncaught Exception in thread ${t.name}",e)
		}
	}

	companion object {
		private val log = LoggerFactory.getLogger(GameServerApplication::class.java)
	}
}

fun main(args: Array<String>) {
	runApplication<GameServerApplication>(*args)
}