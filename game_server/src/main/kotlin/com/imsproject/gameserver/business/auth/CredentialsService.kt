package com.imsproject.gameserver.business.auth

import com.imsproject.gameserver.dataAccess.implementations.CredentialsDAO
import com.imsproject.gameserver.dataAccess.implementations.CredentialsPK
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class CredentialsService(private val credentialsDAO: CredentialsDAO) {

    val credentials = ConcurrentHashMap<String,String>()

    init {
        credentials.put("admin", "\$2a\$10\$3Dw.gqCvW3tbdHc7qGSLr.4ry49vTMebhTKxu/J5zcNrEBKi4BuGG")
    }

    operator fun get(userId: String): String? {
        if(!contains(userId)) return null
        return credentials[userId] ?: let {
            val selected = credentialsDAO.select(CredentialsPK(userId))
            credentials[userId] = selected.password
            selected.password
        }
    }

    operator fun contains(userId: String): Boolean {
        return credentials.containsKey(userId) || credentialsDAO.exists(CredentialsPK(userId))
    }

    operator fun set(userId: String, password: String) {
        if(contains(userId)) throw UnsupportedOperationException("Updating credentials is not supported. Use remove() and set() instead.")
        credentialsDAO.insert(Credentials(userId,password))
        this.credentials[userId] = password
    }

    fun getAllUserIds(): List<String>{
        val allCredentials = credentialsDAO.selectAll()
        allCredentials.forEach { credentials[it.userId] = it.password }
        return credentials.keys.toList().filter { it != "admin" }
    }

    fun remove(userId: String) : Boolean {
        if(userId == "admin") {
            throw IllegalArgumentException("Cannot delete admin user")
        }
        if(!contains(userId)) {
            return false
        }
        credentials.remove(userId)
        credentialsDAO.delete(CredentialsPK(userId))
        return true
    }
}