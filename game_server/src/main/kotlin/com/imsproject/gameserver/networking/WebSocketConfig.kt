package com.imsproject.gameserver.networking

import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val gameRequestHandler: GameRequestHandler,
    private val managerEventsHandler: ManagerEventsHandler
) : WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(gameRequestHandler, "/ws").setAllowedOrigins("*")
        registry.addHandler(managerEventsHandler, "/manager/events").setAllowedOrigins("*")
    }
}

