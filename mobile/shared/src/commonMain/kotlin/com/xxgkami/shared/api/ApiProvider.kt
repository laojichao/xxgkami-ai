package com.xxgkami.shared.api

import kotlinx.serialization.json.Json

/**
 * API客户端提供者（单例）
 * 全局统一管理 ApiClient 实例，提供 Token 设置、刷新回调、登出回调等配置方法
 * 所有 API 调用应通过此对象获取客户端实例
 */
object ApiProvider {
    // 全局共享的 JSON 序列化配置，避免各 API 类重复创建实例导致配置不一致
    val json: Json = Json { ignoreUnknownKeys = true; isLenient = true }

    // 全局共享的 API 客户端实例
    val apiClient: ApiClient = ApiClient()

    /** 更新 API 基础地址，必须以 http:// 或 https:// 开头 */
    fun updateBaseUrl(url: String) {
        require(url.isNotBlank()) { "API base URL must not be blank" }
        require(url.startsWith("http://") || url.startsWith("https://")) {
            "API base URL must start with http:// or https://, got: $url"
        }
        apiClient.baseUrl = url
    }

    /** 设置访问令牌和刷新令牌 */
    fun setTokens(accessToken: String?, refreshToken: String?) {
        apiClient.setTokens(accessToken, refreshToken)
    }

    /** 清除所有令牌 */
    fun clearTokens() {
        apiClient.clearTokens()
    }

    /** 设置令牌刷新成功回调，当 Token 自动刷新后触发 */
    fun setOnTokenRefreshed(callback: (newToken: String, newRefreshToken: String) -> Unit) {
        apiClient.onTokenRefreshed = callback
    }

    /** 设置强制登出回调，当令牌刷新失败时触发 */
    fun setOnLogout(callback: () -> Unit) {
        apiClient.onLogout = callback
    }
}
