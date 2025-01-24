package com.imsproject.gameserver.business.auth

import org.springframework.stereotype.Component
import java.io.File
import java.util.concurrent.ConcurrentHashMap

private const val USERS_PATH = "/app/data/users/"

@Component
class CredentialsController {

    val credentials = ConcurrentHashMap<String,Credentials>()

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