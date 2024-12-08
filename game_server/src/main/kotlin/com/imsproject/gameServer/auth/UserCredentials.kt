package com.imsproject.gameServer.auth

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class UserCredentials(private val userId: String, private val password: String) : UserDetails {
    override fun getAuthorities(): Collection<GrantedAuthority> {
        return listOf()
    }

    override fun getPassword(): String {
        return password
    }

    override fun getUsername(): String {
        return userId
    }
}
