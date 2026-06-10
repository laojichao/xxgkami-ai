package com.xxgkami.shared.util

/**
 * 跨平台Token安全持久化存储 expect声明
 *
 * 各平台提供 actual 实现：
 * - Android: EncryptedSharedPreferences（通过现有TokenStore包装）
 * - iOS: Keychain Services
 * - Desktop: AES-GCM加密文件存储
 *
 * 用于在应用重启后恢复用户登录状态，避免每次启动都需要重新登录。
 */
expect class PlatformTokenStore() {
    /** 保存访问令牌和刷新令牌到安全存储 */
    fun saveTokens(accessToken: String, refreshToken: String)

    /** 获取访问令牌，未登录或已过期返回 null */
    fun getAccessToken(): String?

    /** 获取刷新令牌，未登录或已过期返回 null */
    fun getRefreshToken(): String?

    /** 清除所有已存储的令牌（登出时调用） */
    fun clearTokens()

    /** 判断当前是否有有效的访问令牌（即用户是否已登录） */
    fun isLoggedIn(): Boolean
}
