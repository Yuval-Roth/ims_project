package com.imsproject.gameserver.auth

data class Credentials(
    val userId: String,
    val password: String,
    val token: String? = null,
    val newPassword: String? = null
)

