package com.imsproject.servermanager

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain

@EnableWebSecurity
@Configuration
class SecurityConfig {
    @Bean
    @Throws(Exception::class)
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers("/error-reports/add").permitAll()
                    .anyRequest().authenticated()
            }
            .csrf { csrf ->
                csrf.ignoringRequestMatchers("/error-reports/add")
            }
            .formLogin(Customizer.withDefaults())
            .httpBasic{ obj -> obj.disable() }
            .authenticationManager(object : AuthenticationManager {

                private val encoder: PasswordEncoder = BCryptPasswordEncoder()
                private val pass = "\$2a\$10\$Ss09W28r0vuNd67EHqcAw.piDzMvPFV4YHK0d0rh2C30O26NYAewG"

                override fun authenticate(authentication: Authentication): Authentication {
                    val userName = authentication.name
                    if(userName.lowercase() != "admin") {
                        throw BadCredentialsException("Bad Credentials")
                    }
                    val password = authentication.credentials.toString()
                    if (encoder.matches(password, pass)) {
                        return UsernamePasswordAuthenticationToken(userName, password, emptyList())
                    } else {
                        throw BadCredentialsException("Bad Credentials")
                    }
                }
            })
        return http.build()
    }
}
