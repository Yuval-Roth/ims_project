package com.imsproject.gameserver.business.auth

import com.imsproject.common.utils.Response
import com.imsproject.common.utils.toJson
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
        val jwt = extractJwtFromHeader(request.getHeader("Authorization"))
        if(jwt != null){
            try{
                val authentication = jwtController.extractAuthentication(jwt)
                val userId = authentication.principal
                if(authController.userExists(userId)){
                    SecurityContextHolder.getContext().authentication = authentication
                }
            } catch (_: ExpiredJwtException) {
                log.debug("expired JWT for request to ${request.requestURI}")
                rejectJwt(response)
                return
            } catch(_:Exception){
                log.debug("Invalid JWT for request to ${request.requestURI}")
                rejectJwt(response)
                return
            }
        }
        filterChain.doFilter(request, response)
    }

    private fun rejectJwt(response: HttpServletResponse) {
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = "application/json"
        response.writer.write(Response.getError("Invalid Bearer token").toJson())
    }

    private fun extractJwtFromHeader(header: String?): String? {
        return if (header?.startsWith("Bearer ") == true) header.removePrefix("Bearer ") else null
    }

    companion object {
        private val log = LoggerFactory.getLogger(JwtFilter::class.java)
    }
}