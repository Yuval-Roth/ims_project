package com.imsproject.gameserver.business.auth

import com.imsproject.common.utils.Response
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val credentials: CredentialsService,
    private val jwtService: JwtService,
) {
    private val encoder: PasswordEncoder = BCryptPasswordEncoder()

    fun authenticateUser(userId: String, password: String): String {
        val cleanUserId = userId.lowercase()
        log.debug("Authenticating user {}", cleanUserId)
        val answer = authenticate(cleanUserId, password)
        return if(answer){
            Response.getOk(jwtService.generateJwt(cleanUserId))
        } else {
            Response.getError("Invalid credentials")
        }
    }

    //================================================================================= |
    //============================== USER MANAGEMENT ================================== |
    //================================================================================= |

    fun createUser(credentials: Credentials) {
        val cleanUserId = credentials.userId.lowercase()
        val rawPassword = credentials.password
        if (userExists(cleanUserId)) {
            throw IllegalArgumentException("user already exists")
        }
        if(!isValidPassword(rawPassword)) {
            throw IllegalArgumentException("Password does not meet the requirements")
        }

        log.debug("Adding user credentials for user {}", cleanUserId)
        val hashedPassword = encoder.encode(rawPassword)
        this.credentials[cleanUserId] = hashedPassword
    }

    fun deleteUser(userId: String) {
        val cleanUserId = userId.lowercase()
        if (!userExists(cleanUserId)) {
            throw IllegalArgumentException("User not found")
        }
        credentials.remove(cleanUserId)
    }

    fun userExists(userId: String): Boolean {
        return userId in credentials
    }

    fun getAllUsers() : List<String> {
        return credentials.getAllUserIds()
    }

    fun textToBCrypt(text: String): String {
        return encoder.encode(text)
    }

    //============================================================================ |
    //========================= PRIVATE METHODS ================================== |
    //============================================================================ |

    private fun isValidPassword(password: String): Boolean {
        val pattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[A-Za-z\\d!@#$%^&*()-=_+\\[\\]{};:<>?/~\\\\|]{8,}$"
        return password.matches(Regex(pattern))
    }

    private fun authenticate(userId: String, password: String): Boolean {
        log.debug("Authenticating user: {}", userId)
        val hashedPassword = credentials[userId] ?: run {
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

    companion object {
        private val log: Logger = LoggerFactory.getLogger(AuthService::class.java)
    }
}
