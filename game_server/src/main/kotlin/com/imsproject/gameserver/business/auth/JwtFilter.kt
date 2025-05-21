package com.imsproject.gameserver.business.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
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
        val auth = request.getHeader("Authorization")
        if (auth != null) {
            if (auth.startsWith("Bearer ")) {
                val token = auth.substring(7)
                if (jwtController.isAuthentic(token)) {
                    val userId = jwtController.extractUserId(token)
                    if(authController.userExists(userId)){
                        SecurityContextHolder.getContext().authentication = JwtAuthentication(userId)
                    }
                }
            }
        }
        filterChain.doFilter(request, response)
    }
}