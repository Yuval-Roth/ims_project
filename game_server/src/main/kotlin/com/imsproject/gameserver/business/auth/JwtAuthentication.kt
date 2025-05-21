package com.imsproject.gameserver.business.auth

import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority

class JwtAuthentication(
    private val userId: String,
    roles: List<String>
): AbstractAuthenticationToken(roles.map { SimpleGrantedAuthority(it) } ) {

    init {
        isAuthenticated = true
    }

    override fun getCredentials() = null

    override fun getPrincipal() = userId
}