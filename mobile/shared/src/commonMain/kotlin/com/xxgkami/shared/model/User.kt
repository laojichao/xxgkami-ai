package com.xxgkami.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Int? = null,
    val username: String? = null,
    val email: String? = null,
    val nickname: String? = null,
    val avatar: String? = null,
    val phone: String? = null,
    val status: Boolean? = null,
    val role: String? = null,
    val createTime: String? = null
)

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class RegisterRequest(val username: String, val password: String, val email: String, val code: String)

@Serializable
data class LoginResponse(
    val token: String? = null,
    val refreshToken: String? = null,
    val userInfo: User? = null
)
