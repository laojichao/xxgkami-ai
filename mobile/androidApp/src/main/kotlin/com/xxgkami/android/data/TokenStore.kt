package com.xxgkami.android.data

import android.content.Context
import android.content.SharedPreferences
import com.xxgkami.shared.api.ApiProvider

object TokenStore {
    private const val PREFS_NAME = "xxgkami_auth"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_USERNAME = "username"
    private const val KEY_ROLE = "role"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Restore tokens from storage
        val accessToken = prefs?.getString(KEY_ACCESS_TOKEN, null)
        val refreshToken = prefs?.getString(KEY_REFRESH_TOKEN, null)
        if (accessToken != null) {
            ApiProvider.setTokens(accessToken, refreshToken)
        }

        // Save tokens when refreshed
        ApiProvider.setOnTokenRefreshed { newToken, newRefreshToken ->
            saveTokens(newToken, newRefreshToken)
        }

        // Clear tokens on logout
        ApiProvider.setOnLogout {
            clearTokens()
        }
    }

    fun saveTokens(accessToken: String, refreshToken: String) {
        prefs?.edit()?.apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            apply()
        }
        ApiProvider.setTokens(accessToken, refreshToken)
    }

    fun saveUserInfo(username: String, role: String) {
        prefs?.edit()?.apply {
            putString(KEY_USERNAME, username)
            putString(KEY_ROLE, role)
            apply()
        }
    }

    fun getAccessToken(): String? = prefs?.getString(KEY_ACCESS_TOKEN, null)
    fun getRefreshToken(): String? = prefs?.getString(KEY_REFRESH_TOKEN, null)
    fun getUsername(): String? = prefs?.getString(KEY_USERNAME, null)
    fun getRole(): String? = prefs?.getString(KEY_ROLE, null)
    fun isLoggedIn(): Boolean = getAccessToken() != null

    fun clearTokens() {
        prefs?.edit()?.clear()?.apply()
        ApiProvider.clearTokens()
    }
}
