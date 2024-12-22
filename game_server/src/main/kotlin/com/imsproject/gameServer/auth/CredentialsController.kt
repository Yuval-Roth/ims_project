package com.imsproject.gameServer.auth

import org.springframework.stereotype.Component
import java.io.File

private const val USERS_PATH = "/app/data/users/"

@Component
class CredentialsController {

    operator fun get(userId: String): Credentials? {
        return if(userId in this){
            val file = File(userId.toFilePath())
            Credentials(userId, file.readLines()[0])
        } else {
            null
        }
    }

    operator fun contains(userId: String): Boolean {
        val file = File(userId.toFilePath())
        return file.exists()
    }

    operator fun set(userId: String, credentials: Credentials) {
        val file = File(userId.toFilePath())
        if(userId in this) {
            file.delete()
        }
        file.mkdirs()
        file.createNewFile()
        file.writeText(credentials.password)
    }

    fun remove(userId: String) : Boolean {
        if(userId in this){
            val file = File(userId.toFilePath())
            file.delete()
            return true
        }
        return false
    }

    private fun String.toFilePath() = "$USERS_PATH$this"
}