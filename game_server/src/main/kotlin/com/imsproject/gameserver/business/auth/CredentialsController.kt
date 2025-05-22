package com.imsproject.gameserver.business.auth

import org.springframework.stereotype.Component
import java.io.File
import java.util.concurrent.ConcurrentHashMap

private const val USERS_PATH = "/app/data/users/"

@Component
class CredentialsController {

    val credentials = ConcurrentHashMap<String,Credentials>()

    init {
        credentials.put("admin",Credentials("admin","\$2a\$10\$3Dw.gqCvW3tbdHc7qGSLr.4ry49vTMebhTKxu/J5zcNrEBKi4BuGG",null))
        credentials.put("user",Credentials("user","\$2a\$10\$3Dw.gqCvW3tbdHc7qGSLr.4ry49vTMebhTKxu/J5zcNrEBKi4BuGG",null))
    }

    operator fun get(userId: String): Credentials? {
        return credentials[userId]
    }

    operator fun contains(userId: String): Boolean {
        return credentials.containsKey(userId)
    }

    operator fun set(userId: String, credentials: Credentials) {
        this.credentials[userId] = credentials
    }

    fun getAll(): List<String>{
        return credentials.keys().toList()
    }

    fun remove(userId: String) : Boolean {
        return credentials.remove(userId) != null
    }
}