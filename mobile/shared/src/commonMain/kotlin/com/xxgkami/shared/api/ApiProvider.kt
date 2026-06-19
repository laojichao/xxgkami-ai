package com.xxgkami.shared.api

import kotlinx.serialization.json.Json
import io.ktor.http.URL
import io.ktor.http.Url

/**
 * API客户端提供者（单例）
 * 全局统一管理 ApiClient 实例，提供 Token 设置、刷新回调、登出回调等配置方法
 * 所有 API 调用应通过此对象获取客户端实例
 */
object ApiProvider {
    // 全局共享的 JSON 序列化配置，避免各 API 类重复创建实例导致配置不一致
    // isLenient = true 允许将 JSON 数字（如 BigDecimal 100.00）反序列化为 String 类型
    val json: Json = Json { ignoreUnknownKeys = true; isLenient = true }

    // 全局共享的 API 客户端实例（默认无引擎，updateBaseUrl 时按平台注入证书锁定引擎）
    val apiClient: ApiClient = ApiClient()

    /**
     * 允许明文 HTTP 访问的本地开发主机名白名单。
     * 仅这些主机允许使用 http:// 协议，其他主机必须使用 https://。
     */
    private val HTTP_ALLOWED_HOSTS = setOf("localhost", "10.0.2.2", "127.0.0.1")

    /**
     * 更新 API 基础地址，必须以 http:// 或 https:// 开头。生产环境必须使用 HTTPS。
     * 对于 HTTPS 地址，自动注入平台相关的证书锁定引擎（Android 上为 OkHttp CertificatePinner）。
     *
     * @param url API 基础地址
     * @throws IllegalArgumentException 当 URL 格式非法或生产环境使用明文 HTTP 时抛出
     */
    fun updateBaseUrl(url: String) {
        require(url.isNotBlank()) { "API base URL must not be blank" }
        // 使用 Ktor URL 严格解析主机名，避免 contains 绕过（如 "https://evil.com/localhost"）
        val parsed: Url = try {
            URL(url)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid API base URL: $url", e)
        }
        val scheme = parsed.protocol.name
        val host = parsed.host
        require(scheme == "https" || (scheme == "http" && host in HTTP_ALLOWED_HOSTS)) {
            "Production API base URL must use HTTPS. Got: $url"
        }
        // 对 HTTPS URL 注入平台证书锁定引擎（仅在首次设置时）
        if (scheme == "https" && apiClient.engine == null && host.isNotEmpty()) {
            try {
                apiClient.engine = createPlatformEngine(host)
            } catch (_: Exception) {
                // 引擎创建失败时不阻塞，退回到默认引擎
            }
        }
        apiClient.updateBaseUrl(url)
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
