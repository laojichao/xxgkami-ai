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
     *
     * 注意：[httpClient] 采用懒加载，首次访问后引擎即被固化。
     * 若在首次请求后修改 [engine]，将抛出 [IllegalStateException] 以避免配置不一致。
     */
    @Volatile var engine: HttpClientEngine? = null
        set(value) {
            check(!httpClientInitialized) {
                "Cannot change engine after HttpClient has been initialized. Set engine before any HTTP request."
            }
            field = value
        }

    /** 标记 httpClient 是否已初始化，用于检测 engine 变更时序问题 */
    @Volatile private var httpClientInitialized = false

    /** HTTP 客户端实例，懒加载初始化 */
    @Volatile private var _httpClient: HttpClient? = null

    private fun requireBaseUrl(): String {
        return baseUrl.ifBlank { throw IllegalStateException("API base URL not configured. Call ApiProvider.updateBaseUrl() first.") }
    }

    /** 互斥锁，防止多个并发请求同时触发 Token 刷新 */
    private val refreshMutex = Mutex()

    /** 获取 HTTP 客户端实例，线程安全地懒加载初始化 */
    private fun httpClient(): HttpClient {
        _httpClient?.let { return it }
        synchronized(this) {
            _httpClient?.let { return it }
            val client = createHttpClient()
            _httpClient = client
            httpClientInitialized = true
            return client
        }
    }

    /** 创建 HTTP 客户端实例，根据 [engine] 是否设置决定引擎配置 */
    private fun createHttpClient(): HttpClient {
        val config: HttpClientConfig<*>.() -> Unit = {
            // 统一使用 ApiProvider.json，避免重复创建 JSON 实例导致配置不一致
            install(ContentNegotiation) { json(ApiProvider.json) }
            install(HttpTimeout) {
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 10000
            }
            // 安装 DefaultRequest 插件，统一添加 User-Agent 和 API 版本头
            install(DefaultRequest) {
                header(HttpHeaders.UserAgent, USER_AGENT)
                header(X_API_VERSION, API_VERSION)
            }
        }
        val e = engine
        return if (e != null) HttpClient(e, config) else HttpClient(config)
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
        // 在获取锁之前记录当前 token，用于获取锁后判断是否已被其他协程刷新
        val tokenBeforeLock = token
        return refreshMutex.withLock {
            // 获取锁后二次检查：如果 refreshToken 已被清除（如其他协程登出），直接返回失败
            if (refreshToken == null) return@withLock false
            // 如果 token 已变化（被其他协程刷新成功），直接返回成功，避免重复刷新
            if (token != null && token != tokenBeforeLock) return@withLock true

            try {
                val body = buildJsonObject {
                    put("refreshToken", JsonPrimitive(currentRefreshToken))
                }.toString()

                val response = httpClient().post("${requireBaseUrl()}/auth/refresh") {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }

                if (response.status == HttpStatusCode.OK) {
                    val responseText = response.bodyAsText()
                    val jsonResponse = ApiProvider.json.parseToJsonElement(responseText).jsonObject
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
     * **幂等性说明**：仅对 GET 请求自动重试（GET 语义上幂等），
     * POST/PUT/DELETE 等非幂等请求不自动重试，避免重复提交导致数据不一致。
     *
     * @param request 待执行的 HTTP 请求 lambda
     * @param isIdempotent 请求是否幂等（仅幂等请求在 401 后自动重试）
     * @return 响应体字符串
     * @throws ClientRequestException 请求失败且无法通过刷新 Token 恢复时抛出
     */
    private suspend fun executeWithRefresh(request: suspend () -> String, isIdempotent: Boolean): String {
        // 首次尝试执行请求
        try {
            return request()
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.Unauthorized) {
                // 收到 401，尝试刷新 Token
                val refreshed = tryRefreshToken()
                if (refreshed) {
                    // 仅对幂等请求（如 GET）自动重试，避免非幂等请求重复提交
                    if (isIdempotent) {
                        return request()
                    }
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
            // bodyAsText 可能抛异常（如响应体为空或连接已关闭），捕获后使用空字符串
            val bodyText = try {
                response.bodyAsText()
            } catch (_: Exception) {
                ""
            }
            throw ClientRequestException(response, bodyText)
        }
    }

    /**
     * 发送 GET 请求。
     *
     * @param path 请求路径（不含 baseUrl），例如 "/user/info"
     * @return 响应体字符串
     */
    suspend fun get(path: String): String {
        return executeWithRefresh(isIdempotent = true) {
            val response = httpClient().get("${requireBaseUrl()}$path") {
                token?.let { header("Authorization", "Bearer $it") }
            }
            checkResponseStatus(response)
            response.bodyAsText()
        }
    }

    /**
     * 发送 POST 请求（JSON 请求体）。
     *
     * 注意：POST 为非幂等请求，401 后不会自动重试，避免重复提交。
     *
     * @param path 请求路径（不含 baseUrl）
     * @param body JSON 格式的请求体字符串
     * @return 响应体字符串
     */
    suspend fun post(path: String, body: String): String {
        return executeWithRefresh(isIdempotent = false) {
            val response = httpClient().post("${requireBaseUrl()}$path") {
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
     * 注意：PUT 为非幂等请求，401 后不会自动重试。
     *
     * @param path 请求路径（不含 baseUrl）
     * @param body JSON 格式的请求体字符串
     * @return 响应体字符串
     */
    suspend fun put(path: String, body: String): String {
        return executeWithRefresh(isIdempotent = false) {
            val response = httpClient().put("${requireBaseUrl()}$path") {
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
     * 注意：DELETE 为非幂等请求，401 后不会自动重试。
     *
     * @param path 请求路径（不含 baseUrl）
     * @return 响应体字符串
     */
    suspend fun delete(path: String): String {
        return executeWithRefresh(isIdempotent = false) {
            val response = httpClient().delete("${requireBaseUrl()}$path") {
                token?.let { header("Authorization", "Bearer $it") }
            }
            checkResponseStatus(response)
            response.bodyAsText()
        }
    }

    companion object {
        /** 应用 User-Agent 标识，用于服务端识别客户端类型和版本 */
        private const val USER_AGENT = "XXGKami-Mobile/1.0.2 (Android;iOS;Desktop)"

        /** API 版本号，用于服务端兼容性处理 */
        private const val API_VERSION = "v1"

        /** 自定义 API 版本头名称 */
        private const val X_API_VERSION = "X-API-Version"
    }
}
