package com.xxgkami.shared.util

class TokenManager {
    var accessToken: String? = null
    var refreshToken: String? = null
    var username: String? = null
    var role: String? = null

    fun isLoggedIn(): Boolean = accessToken != null
    fun isAdmin(): Boolean = role == "admin"

    fun clear() {
        accessToken = null
        refreshToken = null
        username = null
        role = null
    }
}
