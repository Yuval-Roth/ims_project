package com.imsproject.gameserver.business.auth

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@EnableWebSecurity
@Configuration
class SecurityConfig(
    private val jwtFilter: JwtFilter
) {
    @Bean
    @Throws(Exception::class)
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { it.configurationSource(corsConfigurationSource()) }
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers(
                        "/bcrypt/**",
                        "/error",
                        "/login",
                        "/data/session/insert/events",
                        "/data/session/insert/feedback",
                        "/data/experiment/insert/feedback",
                        "/ws",
                    ).permitAll()
                    .anyRequest().authenticated()
            }
            .formLogin { obj -> obj.disable() }
            .csrf { obj -> obj.disable() }
            .httpBasic{ obj -> obj.disable() }
            .sessionManagement { sessionManagement ->
                sessionManagement.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            }
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf(
            "http://localhost:5000",
            "https://ims-project.cs.bgu.ac.il",
            "https://ims-project.cs.bgu.ac.il:8640"
        )
        configuration.allowedMethods = listOf("GET", "POST")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
