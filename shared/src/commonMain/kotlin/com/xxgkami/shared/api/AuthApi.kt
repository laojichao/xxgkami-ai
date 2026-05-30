package com.xxgkami.shared.api

import com.xxgkami.shared.model.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

class AuthApi(private val client: ApiClient) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun adminLogin(request: LoginRequest): ApiResponse<LoginResponse> {
        val response = client.post("/auth/admin/login", json.encodeToString(LoginRequest.serializer(), request))
        return json.decodeFromString(ApiResponse.serializer(LoginResponse.serializer()), response)
    }

    suspend fun userLogin(request: LoginRequest): ApiResponse<LoginResponse> {
        val response = client.post("/auth/user/login", json.encodeToString(LoginRequest.serializer(), request))
        return json.decodeFromString(ApiResponse.serializer(LoginResponse.serializer()), response)
    }

    suspend fun register(request: RegisterRequest): ApiResponse<Unit> {
        val response = client.post("/auth/register", json.encodeToString(RegisterRequest.serializer(), request))
        return json.decodeFromString(ApiResponse.serializer(kotlinx.serialization.builtins.serializer<Unit>()), response)
    }

    suspend fun sendEmailCode(email: String, type: String): ApiResponse<Unit> {
        val body = buildJsonObject {
            put("email", JsonPrimitive(email))
            put("type", JsonPrimitive(type))
        }.toString()
        val response = client.post("/auth/email-code", body)
        return json.decodeFromString(ApiResponse.serializer(kotlinx.serialization.builtins.serializer<Unit>()), response)
    }

    suspend fun refreshToken(refreshToken: String): ApiResponse<LoginResponse> {
        val body = buildJsonObject {
            put("refreshToken", JsonPrimitive(refreshToken))
        }.toString()
        val response = client.post("/auth/refresh", body)
        return json.decodeFromString(ApiResponse.serializer(LoginResponse.serializer()), response)
    }

    suspend fun logout(id: Int, role: String): ApiResponse<Unit> {
        val body = buildJsonObject {
            put("id", JsonPrimitive(id))
            put("role", JsonPrimitive(role))
        }.toString()
        val response = client.post("/auth/logout", body)
        return json.decodeFromString(ApiResponse.serializer(kotlinx.serialization.builtins.serializer<Unit>()), response)
    }

    suspend fun getUserInfo(): ApiResponse<User> {
        val response = client.get("/auth/user/info")
        return json.decodeFromString(ApiResponse.serializer(User.serializer()), response)
    }
}
