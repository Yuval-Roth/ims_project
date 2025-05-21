package com.imsproject.gameserver.business.auth;

import org.springframework.stereotype.Service;
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.MacAlgorithm
import org.slf4j.LoggerFactory
import javax.crypto.SecretKey


@Service
class JwtController {
    private var key: SecretKey = Jwts.SIG.HS512.key().build()
    private var jwtParser = Jwts.parser().verifyWith(key).build()

    fun generateJwt(userId: String): String {
        val cleanUserId = userId.lowercase()
        return Jwts.builder()
                .subject(cleanUserId)
                .signWith(key, alg)
                .compact()
    }

    fun extractUserId(jwt: String): String {
        return jwtParser.parseSignedClaims(jwt)
            .payload
            .subject
            .lowercase()
    }

    fun isOwner(jwt: String, userId: String): Boolean {
        val cleanUserId = userId.lowercase()
        try {
            val userIdFromToken = extractUserId(jwt)
            return cleanUserId == userIdFromToken
        } catch (_: Exception) {
            return false
        }
    }

    fun isAuthentic(jwt: String): Boolean {
        return try {
            extractUserId(jwt)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * This operation logs out all users by resetting the secret key
     */
    fun resetSecretKey() {
        log.debug("Resetting secret key")
        key = Jwts.SIG.HS512.key().build()
        jwtParser = Jwts.parser().verifyWith(key).build()
    }

    companion object {
        private val log = LoggerFactory.getLogger(JwtController::class.java)
        private val alg: MacAlgorithm = Jwts.SIG.HS512
    }
}
