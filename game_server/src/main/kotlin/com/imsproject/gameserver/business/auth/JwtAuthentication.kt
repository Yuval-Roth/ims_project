package com.imsproject.gameserver.business.auth

import org.springframework.security.authentication.AbstractAuthenticationToken

class JwtAuthentication(
    private val userId: String
): AbstractAuthenticationToken(listOf()) {

    init {
        isAuthenticated = true
    }

    override fun getCredentials() = null

    override fun getPrincipal() = userId
}