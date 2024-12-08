package com.imsproject.gameServer.auth

import com.imsproject.common.utils.Response
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.MacAlgorithm
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.SecretKey

@Component
class AuthController : AuthenticationManager {
    private val userIdToUUID: MutableMap<String, String> = ConcurrentHashMap()
    private val encoder: PasswordEncoder = BCryptPasswordEncoder()
    private var key: SecretKey = Jwts.SIG.HS512.key().build()

    // userId -> UserCredentials (userId, hashedPassword)
    private val credentials: MutableMap<String, UserCredentials> = ConcurrentHashMap()

    fun authenticateUser(userId: String, password: String): Response {
        val cleanUserId = userId.lowercase()
        log.debug("Authenticating user {}", cleanUserId)
        val answer = authenticate(cleanUserId, password)
        return Response(null, answer, if (answer) getToken(cleanUserId) else null)
    }

    fun revokeAuthentication(userId: String): Boolean {
        val cleanUserId = userId.lowercase()
        log.debug("Revoking authentication for user {}", cleanUserId)
        val uuid = userIdToUUID.remove(cleanUserId)
        return uuid != null
    }

    fun validateTokenOwnership(userId: String, token: String): Boolean {
        val cleanUserId = userId.lowercase()
        log.debug("Validating token ownership for user {}", cleanUserId)
        var answer: Boolean
        try {
            val payload = extractPayload(token)
            val userIdFromToken = payload.first

            log.trace("Checking if the UUID from the token matches the stored UUID for user {}", cleanUserId)
            val uuid = userIdToUUID[cleanUserId]
            answer = uuid != null && cleanUserId == userIdFromToken
        } catch (ignored: Exception) {
            answer = false
        }
        log.debug("Token ownership validation for user {} was {}", cleanUserId, if (answer) "successful" else "unsuccessful")
        return answer
    }

    fun validateTokenAuthenticity(token: String): Boolean {
        var answer: Boolean
        try {
            val payload = extractPayload(token)
            val userIdFromToken = payload.first
            val uuidFromToken = payload.second
            val UUIDFromDB = userIdToUUID[userIdFromToken]
            answer = userExists(userIdFromToken) && UUIDFromDB != null && UUIDFromDB == uuidFromToken
        } catch (ignored: Exception) {
            answer = false
        }
        return answer
    }

    /**
     * This operation logs out all users by resetting the secret key
     */
    fun resetSecretKey() {
        log.debug("Resetting secret key")
        key = Jwts.SIG.HS512.key().build()
        userIdToUUID.clear()
    }

    //================================================================================= |
    //=========================== AUTHENTICATION MANAGER ============================== |
    //================================================================================= |
    
    @Throws(AuthenticationException::class)
    override fun authenticate(authentication: Authentication): Authentication {
        val cleanUserId = authentication.name.lowercase()
        val user = credentials[cleanUserId] ?: throw UsernameNotFoundException("User not found")
        val credentials = authentication.credentials as String
        if (authenticate(cleanUserId, credentials)) {
            return UsernamePasswordAuthenticationToken(user, null, null)
        } else {
            throw AccessDeniedException("Bad credentials")
        }
    }

    //================================================================================= |
    //============================== USER MANAGEMENT ================================== |
    //================================================================================= |

    fun createUser(user: UserDetails) {
        val userId = user.username.lowercase()
        if (userExists(userId)) {
            throw AccessDeniedException("user already exists")
        }

        log.debug("Adding user credentials for user {}", userId)
        val hashedPassword = encoder.encode(user.password)
        val userCredentials = UserCredentials(userId, hashedPassword)
        credentials[user.username] = userCredentials
    }

    fun updateUser(user: UserDetails) {
        val userId = user.username.lowercase()
        if (!userExists(userId)) {
            throw UsernameNotFoundException("User not found")
        }

        val hashedPassword = encoder.encode(user.password)
        val updatedUserCredentials = UserCredentials(userId, hashedPassword)
        credentials[user.username] = updatedUserCredentials
    }

    fun deleteUser(userId: String) {
        val cleanUserId = userId.lowercase()
        if (!userExists(cleanUserId)) {
            throw UsernameNotFoundException("User not found")
        }

        credentials.remove(cleanUserId)
    }

    fun userExists(username: String): Boolean {
        return credentials.containsKey(username)
    }

    //============================================================================ |
    //========================= PRIVATE METHODS ================================== |
    //============================================================================ |
    private fun authenticate(userId: String, password: String): Boolean {
        log.debug("Authenticating user: {}", userId)
        val user = credentials[userId] ?: return false
        val hashedPassword = user.hashedPassword

        // check if the user exists
        if (hashedPassword == null) {
            log.debug("User {} does not exist", userId)
            return false
        }
        log.trace("User {} exists", userId)

        // check if the password is correct
        if (!isPasswordsMatch(password, hashedPassword)) {
            log.debug("Incorrect password for user {}", userId)
            return false
        }

        log.debug("User {} authenticated successfully", userId)
        return true
    }

    private fun isPasswordsMatch(password: String, hashedPassword: String): Boolean {
        return encoder.matches(password, hashedPassword)
    }

    private fun generateJwt(payload: String): String {
        log.debug("Generating token for user {}", payload)
        return Jwts.builder()
            .content(payload, "text/plain")
            .signWith(key, alg)
            .compact()
    }

    private fun getToken(userId: String): String {
        //generate a unique UUID
        log.trace("Generating new UUID for user {}", userId)
        val uuid = UUID.randomUUID().toString()
        val payload = "$userId:$uuid"

        //store the UUID and associate it with the user
        log.trace("Storing new UUID for user {}", userId)
        userIdToUUID[userId] = uuid
        return generateJwt(payload)
    }

    private fun extractPayload(token: String): Pair<String, String> {
        val parts = String(
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedContent(token)
                .payload
        ).split(":")
        return Pair(parts[0], parts[1])
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(AuthController::class.java)

        private val alg: MacAlgorithm = Jwts.SIG.HS512
    }
}
