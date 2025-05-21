package com.imsproject.gameserver.business.auth

import com.imsproject.common.utils.Response
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class AuthController(
    private val credentials: CredentialsController,
    private val jwtController: JwtController,
) {
    private val encoder: PasswordEncoder = BCryptPasswordEncoder()

    fun authenticateUser(userId: String, password: String): String {
        val cleanUserId = userId.lowercase()
        log.debug("Authenticating user {}", cleanUserId)
        val answer = authenticate(cleanUserId, password)
        return if(answer){
            Response.getOk(jwtController.generateJwt(cleanUserId))
        } else {
            Response.getError("Invalid credentials")
        }
    }

    //================================================================================= |
    //============================== USER MANAGEMENT ================================== |
    //================================================================================= |

    fun createUser(credentials: Credentials) {
        val cleanUserId = credentials.userId.lowercase()
        val rawPassword = credentials.password ?: run {
            throw IllegalArgumentException("No password given")
        }
        if (userExists(cleanUserId)) {
            throw IllegalArgumentException("user already exists")
        }
        if(!isValidPassword(rawPassword)) {
            throw IllegalArgumentException("Password does not meet the requirements")
        }

        log.debug("Adding user credentials for user {}", cleanUserId)
        val hashedPassword = encoder.encode(rawPassword)
        val userCredentials = Credentials(cleanUserId, hashedPassword,null)
        this@AuthController.credentials[cleanUserId] = userCredentials
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
        return credentials.getAll()
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
        val user = credentials[userId] ?: run {
            log.debug("User {} does not exist", userId)
            return false
        }
        log.trace("User {} exists", userId)
        val hashedPassword = user.password ?: run {
            throw IllegalArgumentException("No password given")
        }

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
        private val log: Logger = LoggerFactory.getLogger(AuthController::class.java)
    }
}
