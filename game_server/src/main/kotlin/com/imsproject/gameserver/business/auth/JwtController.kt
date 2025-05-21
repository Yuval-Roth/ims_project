package com.imsproject.gameserver.business.auth;

import org.springframework.stereotype.Service;
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.MacAlgorithm
import org.slf4j.LoggerFactory
import java.util.Date
import javax.crypto.SecretKey


@Service
class JwtController {
    private var key: SecretKey = Jwts.SIG.HS512.key().build()
    private var jwtParser = Jwts.parser().verifyWith(key).build()

    fun generateJwt(userId: String): String {
        val cleanUserId = userId.lowercase()
        val roles = mutableListOf("ROLE_USER")
        if(cleanUserId == "admin") roles.add("ROLE_ADMIN")
        val now = Date().time
        val expiration = if("ROLE_ADMIN" in roles){
            Date(now + 30 * 60 * 1000) // 30 mins
        } else {
            Date(now + 24 * 60 * 60 * 1000) // 24 hours
        }
        return Jwts.builder()
                .subject(cleanUserId)
                .claim("roles",roles)
                .signWith(key, alg)
                .expiration(expiration)
                .compact()
    }

    fun extractAuthentication(jwt: String): JwtAuthentication {
        val claims = jwtParser.parseSignedClaims(jwt).payload
        val userId = claims.subject
        val rawRoles = claims["roles"]
        val roles = when (rawRoles) {
            is List<*> -> rawRoles.filterIsInstance<String>()
            else -> emptyList()
        }
        return JwtAuthentication(userId,roles)
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
