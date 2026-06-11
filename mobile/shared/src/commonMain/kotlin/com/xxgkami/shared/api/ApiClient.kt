package com.xxgkami.shared.api

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 跨平台 HTTP 客户端，基于 Ktor 实现。
 *
 * 提供统一的 GET/POST/PUT/DELETE 请求方法，并内置自动 Token 刷新机制：
 * 当请求返回 401 Unauthorized 时，自动使用 refreshToken 获取新 token 并重试请求。
 * 使用 Mutex 保证并发场景下仅执行一次刷新操作。
 *
 * @property baseUrl API 服务基础地址，默认为 Android 模拟器本地回环地址
 * @property token 当前访问令牌，由 [setTokens] 设置
 * @property refreshToken 当前刷新令牌，用于自动续期
 * @property onTokenRefreshed Token 刷新成功后的回调，用于同步持久化存储
 * @property onLogout Token 刷新失败后的登出回调，用于清理本地状态
 */
class ApiClient(baseUrl: String = "") {
    var baseUrl: String = baseUrl
        private set
    @Volatile var token: String? = null
        private set
    @Volatile var refreshToken: String? = null
        private set
    var onTokenRefreshed: ((newToken: String, newRefreshToken: String) -> Unit)? = null
    var onLogout: (() -> Unit)? = null

    /**
     * 可选的自定义 HTTP 引擎（如带证书锁定的 OkHttp 引擎）。
     * 必须在首次 HTTP 请求之前设置。
     */
    var engine: HttpClientEngine? = null

    private fun requireBaseUrl(): String {
        return baseUrl.ifBlank { throw IllegalStateException("API base URL not configured. Call ApiProvider.updateBaseUrl() first.") }
    }

    /** 互斥锁，防止多个并发请求同时触发 Token 刷新 */
    private val refreshMutex = Mutex()

    /** JSON 序列化配置：忽略未知字段、宽松模式 */
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** HTTP 客户端懒加载：首次使用时根据 [engine] 是否已设置来决定引擎配置 */
    private val httpClient: HttpClient by lazy {
        val e = engine
        if (e != null) HttpClient(e) {
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 10000
            }
        } else HttpClient {
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 10000
            }
        }
    }

    /**
     * 设置访问令牌和刷新令牌。
     *
     * @param accessToken 访问令牌，null 表示清除
     * @param refreshTokenValue 刷新令牌，null 表示清除
     */
    fun setTokens(accessToken: String?, refreshTokenValue: String?) {
        token = accessToken
        refreshToken = refreshTokenValue
    }

    /**
     * 清除所有令牌，通常在登出或 Token 失效时调用。
     */
    fun clearTokens() {
        token = null
        refreshToken = null
    }

    /**
     * Update the base URL. Only callable from within the module via ApiProvider.
     */
    fun updateBaseUrl(newBaseUrl: String) {
        baseUrl = newBaseUrl
    }

    /**
     * 尝试使用 refreshToken 刷新 accessToken。
     *
     * 使用 Mutex 加锁，保证并发场景下只执行一次刷新请求。
     * 刷新成功后更新本地 token 并触发 [onTokenRefreshed] 回调；
     * 刷新失败则清除 token 并触发 [onLogout] 回调。
     *
     * @return true 表示刷新成功，false 表示刷新失败
     */
    private suspend fun tryRefreshToken(): Boolean {
        val currentRefreshToken = refreshToken ?: return false
        return refreshMutex.withLock {
            // 获取锁后二次检查：如果 token 已被其他协程刷新（值已变化），直接返回成功
            if (refreshToken == null) return@withLock false
            if (refreshToken != currentRefreshToken) return@withLock true

            try {
                val body = buildJsonObject {
                    put("refreshToken", JsonPrimitive(currentRefreshToken))
                }.toString()

                val response = httpClient.post("${requireBaseUrl()}/auth/refresh") {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }

                if (response.status == HttpStatusCode.OK) {
                    val responseText = response.bodyAsText()
                    val jsonResponse = json.parseToJsonElement(responseText).jsonObject
                    val success = jsonResponse["success"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false

                    if (success) {
                        val data = jsonResponse["data"]?.jsonObject
                        val newToken = data?.get("token")?.jsonPrimitive?.content
                        val newRefreshToken = data?.get("refreshToken")?.jsonPrimitive?.content

                        if (newToken != null) {
                            token = newToken
                            if (newRefreshToken != null) refreshToken = newRefreshToken
                            onTokenRefreshed?.invoke(newToken, newRefreshToken ?: currentRefreshToken)
                            return@withLock true
                        }
                    }
                }

                // Refresh failed - clear tokens
                clearTokens()
                onLogout?.invoke()
                false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                clearTokens()
                onLogout?.invoke()
                false
            }
        }
    }

    /**
     * 带自动 Token 刷新的请求执行器。
     *
     * 首次执行请求，若返回 401 则自动刷新 Token 并重试一次；
     * 刷新失败则抛出原始异常。
     *
     * @param request 待执行的 HTTP 请求 lambda
     * @return 响应体字符串
     * @throws ClientRequestException 请求失败且无法通过刷新 Token 恢复时抛出
     */
    private suspend fun executeWithRefresh(request: suspend () -> String): String {
        // 首次尝试执行请求
        try {
            return request()
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.Unauthorized) {
                // 收到 401，尝试刷新 Token
                val refreshed = tryRefreshToken()
                if (refreshed) {
                    // 刷新成功，使用新 Token 重试请求
                    return request()
                }
                throw e
            }
            throw e
        }
    }

    /**
     * Check HTTP response status and throw ClientRequestException if not successful.
     */
    private suspend fun checkResponseStatus(response: HttpResponse) {
        if (!response.status.isSuccess()) {
            throw ClientRequestException(response, response.bodyAsText())
        }
    }

    /**
     * 发送 GET 请求。
     *
     * @param path 请求路径（不含 baseUrl），例如 "/user/info"
     * @return 响应体字符串
     */
    suspend fun get(path: String): String {
        return executeWithRefresh {
            val response = httpClient.get("${requireBaseUrl()}$path") {
                token?.let { header("Authorization", "Bearer $it") }
            }
            checkResponseStatus(response)
            response.bodyAsText()
        }
    }

    /**
     * 发送 POST 请求（JSON 请求体）。
     *
     * @param path 请求路径（不含 baseUrl）
     * @param body JSON 格式的请求体字符串
     * @return 响应体字符串
     */
    suspend fun post(path: String, body: String): String {
        return executeWithRefresh {
            val response = httpClient.post("${requireBaseUrl()}$path") {
                token?.let { header("Authorization", "Bearer $it") }
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            checkResponseStatus(response)
            response.bodyAsText()
        }
    }

    /**
     * 发送 PUT 请求（JSON 请求体）。
     *
     * @param path 请求路径（不含 baseUrl）
     * @param body JSON 格式的请求体字符串
     * @return 响应体字符串
     */
    suspend fun put(path: String, body: String): String {
        return executeWithRefresh {
            val response = httpClient.put("${requireBaseUrl()}$path") {
                token?.let { header("Authorization", "Bearer $it") }
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            checkResponseStatus(response)
            response.bodyAsText()
        }
    }

    /**
     * 发送 DELETE 请求。
     *
     * @param path 请求路径（不含 baseUrl）
     * @return 响应体字符串
     */
    suspend fun delete(path: String): String {
        return executeWithRefresh {
            val response = httpClient.delete("${requireBaseUrl()}$path") {
                token?.let { header("Authorization", "Bearer $it") }
            }
            checkResponseStatus(response)
            response.bodyAsText()
        }
    }
}
