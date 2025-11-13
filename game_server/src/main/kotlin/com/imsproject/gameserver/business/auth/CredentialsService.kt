package com.imsproject.gameserver.business.auth

import com.imsproject.gameserver.dataAccess.implementations.CredentialsDAO
import com.imsproject.gameserver.dataAccess.implementations.CredentialsPK
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class CredentialsService(private val credentialsDAO: CredentialsDAO) {

    val credentials = ConcurrentHashMap<String,List<String>>()

    init {
        credentials["admin"] = listOf<String>(
            "\$2a\$10\$MxScQ5Kx9so7.sk47wiZlOsDPyWBWRFba/hsbf7AyA7.EYBX.RC3O",
            "\$2a\$10\$HsDtOVDqjrHfFWmuxVuhWeZk/ObxAC8uEbzdKqkClMGCnsUXUppRO"
        )
    }

    operator fun get(userId: String): List<String>? {
        if(!contains(userId)) return null
        return credentials[userId] ?: let {
            val selected = credentialsDAO.select(CredentialsPK(userId))
            val pwList = listOf(selected.password)
            credentials[userId] = pwList
            pwList
        }
    }

    operator fun contains(userId: String): Boolean {
        return credentials.containsKey(userId) || credentialsDAO.exists(CredentialsPK(userId))
    }

    operator fun set(userId: String, password: String) {
        if(userId == "admin") {
            throw IllegalArgumentException("Cannot update admin user")
        }

        if(contains(userId)) throw UnsupportedOperationException("Updating credentials is not supported. Use remove() and set() instead.")
        credentialsDAO.insert(Credentials(userId,password))
        this.credentials[userId] = listOf(password)
    }

    fun getAllUserIds(): List<String> {
        val allCredentials = credentialsDAO.selectAll()
        allCredentials.forEach { credentials[it.userId] = listOf(it.password) }
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