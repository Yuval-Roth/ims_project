package com.imsproject.servermanager

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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
import org.springframework.security.web.authentication.WebAuthenticationDetails
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger


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
                private val badAttemptsMap : MutableMap<String, AtomicInteger> = ConcurrentHashMap()
                private val lockedOutAddresses : MutableMap<String, Job> = ConcurrentHashMap()

                override fun authenticate(authentication: Authentication): Authentication {
                    val attributes = RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes
                    val remoteAddress = attributes.request.getHeader("X-Real-IP")

                    if(lockedOutAddresses.contains(remoteAddress)) {
                        throw BadCredentialsException("Login attempts exceeded, try again later")
                    }

                    try{
                        val userName = authentication.name
                        if(userName.lowercase() != "admin") {
                            badAttemptsMap.computeIfAbsent(remoteAddress) { AtomicInteger(0) }.incrementAndGet()
                            throw BadCredentialsException("Bad Credentials")
                        }
                        val password = authentication.credentials.toString()
                        if (encoder.matches(password, pass)) {
                            badAttemptsMap.remove(remoteAddress)
                            return UsernamePasswordAuthenticationToken(userName, password, emptyList())
                        } else {
                            badAttemptsMap.computeIfAbsent(remoteAddress) { AtomicInteger(0) }.incrementAndGet()
                            throw BadCredentialsException("Bad Credentials")
                        }
                    } finally {
                        val attemptsCount = badAttemptsMap[remoteAddress]
                        if(attemptsCount != null){
                            synchronized(attemptsCount) {
                                if(remoteAddress !in lockedOutAddresses && attemptsCount.get() >= 3) {
                                    @OptIn(DelicateCoroutinesApi::class)
                                    lockedOutAddresses[remoteAddress] = GlobalScope.launch {
                                        kotlinx.coroutines.delay(30 * 1000)
                                        lockedOutAddresses.remove(remoteAddress)
                                        badAttemptsMap.remove(remoteAddress)
                                    }
                                }
                            }
                        }
                    }
                }
            })
        return http.build()
    }
}
