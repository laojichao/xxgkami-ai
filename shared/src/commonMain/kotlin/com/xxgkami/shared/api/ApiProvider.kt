package com.xxgkami.shared.api

object ApiProvider {
    val apiClient: ApiClient = ApiClient()

    fun updateBaseUrl(url: String) {
        apiClient.baseUrl = url
    }

    fun setTokens(accessToken: String?, refreshToken: String?) {
        apiClient.setTokens(accessToken, refreshToken)
    }

    fun clearTokens() {
        apiClient.clearTokens()
    }

    fun setOnTokenRefreshed(callback: (newToken: String, newRefreshToken: String) -> Unit) {
        apiClient.onTokenRefreshed = callback
    }

    fun setOnLogout(callback: () -> Unit) {
        apiClient.onLogout = callback
    }
}
