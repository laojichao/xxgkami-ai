package com.xxgkami.shared.api

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ApiClient(var baseUrl: String = "http://10.0.2.2:8080/api") {
    var token: String? = null
    var refreshToken: String? = null
    var onTokenRefreshed: ((newToken: String, newRefreshToken: String) -> Unit)? = null
    var onLogout: (() -> Unit)? = null

    private val refreshMutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15000
            connectTimeoutMillis = 10000
        }
    }

    fun setTokens(accessToken: String?, refreshTokenValue: String?) {
        token = accessToken
        refreshToken = refreshTokenValue
    }

    fun clearTokens() {
        token = null
        refreshToken = null
    }

    private suspend fun tryRefreshToken(): Boolean {
        val currentRefreshToken = refreshToken ?: return false
        return refreshMutex.withLock {
            // Double-check after acquiring lock
            if (refreshToken == null) return@withLock false

            try {
                val body = buildJsonObject {
                    put("refreshToken", JsonPrimitive(currentRefreshToken))
                }.toString()

                val response = httpClient.post("$baseUrl/auth/refresh") {
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
            } catch (e: Exception) {
                clearTokens()
                onLogout?.invoke()
                false
            }
        }
    }

    private suspend fun executeWithRefresh(request: suspend () -> String): String {
        // First attempt
        try {
            return request()
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.Unauthorized) {
                // Try to refresh token
                val refreshed = tryRefreshToken()
                if (refreshed) {
                    // Retry with new token
                    return request()
                }
                throw e
            }
            throw e
        }
    }

    suspend fun get(path: String): String {
        return executeWithRefresh {
            httpClient.get("$baseUrl$path") {
                token?.let { header("Authorization", "Bearer $it") }
            }.bodyAsText()
        }
    }

    suspend fun post(path: String, body: String): String {
        return executeWithRefresh {
            httpClient.post("$baseUrl$path") {
                token?.let { header("Authorization", "Bearer $it") }
                contentType(ContentType.Application.Json)
                setBody(body)
            }.bodyAsText()
        }
    }

    suspend fun put(path: String, body: String): String {
        return executeWithRefresh {
            httpClient.put("$baseUrl$path") {
                token?.let { header("Authorization", "Bearer $it") }
                contentType(ContentType.Application.Json)
                setBody(body)
            }.bodyAsText()
        }
    }

    suspend fun delete(path: String): String {
        return executeWithRefresh {
            httpClient.delete("$baseUrl$path") {
                token?.let { header("Authorization", "Bearer $it") }
            }.bodyAsText()
        }
    }
}
