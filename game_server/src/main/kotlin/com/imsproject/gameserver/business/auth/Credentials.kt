package com.imsproject.gameserver.business.auth

data class Credentials(
    val userId: String,
    val password: String,
    val token: String?,
    val newPassword: String?
)

