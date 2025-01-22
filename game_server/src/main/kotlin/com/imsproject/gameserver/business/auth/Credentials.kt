package com.imsproject.gameserver.business.auth

data class Credentials(
    val userId: String,
    val password: String,
    val token: String? = null,
    val newPassword: String? = null
)

