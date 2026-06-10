package com.xxgkami.shared.util

import com.xxgkami.android.data.TokenStore

/**
 * Android平台Token持久化 actual实现
 *
 * 包装现有的 TokenStore（基于 EncryptedSharedPreferences），
 * 将 Android 特有的 Context 初始化逻辑保留在 TokenStore 中，
 * 此类仅提供跨平台统一接口。
 */
actual class PlatformTokenStore actual constructor() {

    actual fun saveTokens(accessToken: String, refreshToken: String) {
        TokenStore.saveTokens(accessToken, refreshToken)
    }

    actual fun getAccessToken(): String? = TokenStore.getAccessToken()

    actual fun getRefreshToken(): String? = TokenStore.getRefreshToken()

    actual fun clearTokens() {
        TokenStore.clearTokens()
    }

    actual fun isLoggedIn(): Boolean = TokenStore.isLoggedIn()
}
