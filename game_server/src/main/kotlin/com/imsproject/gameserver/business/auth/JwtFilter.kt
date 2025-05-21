package com.imsproject.gameserver.business.auth

import io.jsonwebtoken.ExpiredJwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.lang.NonNull
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException
import kotlin.text.startsWith
import kotlin.text.substring

@Component
class JwtFilter(
    private val jwtController: JwtController,
    private val authController: AuthController
) : OncePerRequestFilter() {

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(request: HttpServletRequest,
                                  @NonNull response: HttpServletResponse,
                                  @NonNull filterChain: FilterChain
    ) {
        val jwt = extractTokenFromHeader(request.getHeader("Authorization")) ?: run {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing Authorization header")
            log.debug("Missing JWT for request to ${request.requestURI}")
            return
        }
        try{
            val authentication = jwtController.extractAuthentication(jwt)
            val userId = authentication.principal
            if(authController.userExists(userId)){
                SecurityContextHolder.getContext().authentication = authentication
            }

        } catch (e: ExpiredJwtException) {
            log.debug("Expired JWT token: ${e.message}")
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token expired")
            return
        } catch(_:Exception){
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT")
            log.debug("Invalid JWT for request to ${request.requestURI}")
            return
        }
        filterChain.doFilter(request, response)
    }

    private fun extractTokenFromHeader(header: String?): String? {
        return if (header?.startsWith("Bearer ") == true) header.substring(7) else null
    }

    companion object {
        private val log = LoggerFactory.getLogger(JwtFilter::class.java)
    }
}