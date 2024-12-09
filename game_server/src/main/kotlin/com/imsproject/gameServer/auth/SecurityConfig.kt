package com.imsproject.gameServer.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.lang.NonNull
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.intercept.AuthorizationFilter
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException

@EnableWebSecurity
@Configuration
class SecurityConfig(private val authController: AuthController) {
    @Bean
    @Throws(Exception::class)
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { authorize ->
                authorize

                    .anyRequest().permitAll()
//                    //TODO: Remove this when we want to enable security
//                    .requestMatchers("manager","auth","data","ws").permitAll()
//
//
//                    .anyRequest().authenticated()
            }
            .formLogin { obj -> obj.disable() }
            .csrf { obj -> obj.disable() }
            .httpBasic(Customizer.withDefaults())
            .sessionManagement { sessionManagement ->
                sessionManagement.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            }
            .addFilterBefore(JWTFilter(), AuthorizationFilter::class.java)
            .authenticationManager(authController)
        return http.build()
    }

    class JWTAuthentication : UsernamePasswordAuthenticationToken(null, null, null)

    private inner class JWTFilter : OncePerRequestFilter() {
        @Throws(ServletException::class, IOException::class)
        override fun doFilterInternal(request: HttpServletRequest,
            @NonNull response: HttpServletResponse,
            @NonNull filterChain: FilterChain
        ) {
            var token = request.getHeader("Authorization")
            SecurityContextHolder.getContext().authentication = JWTAuthentication() //TODO: Remove this when we want to enable security
//            if (token != null) {
//                if (token.startsWith("Bearer ")) {
//                    token = token.substring(7)
//                    if (authController.validateTokenAuthenticity(token)) {
//                        SecurityContextHolder.getContext().authentication = JWTAuthentication()
//                    }
//                }
//            }
            filterChain.doFilter(request, response)
        }
    }
}
