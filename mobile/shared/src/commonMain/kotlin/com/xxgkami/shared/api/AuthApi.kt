package com.xxgkami.shared.api

import com.xxgkami.shared.model.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * 认证相关API
 * 封装用户登录、注册、邮箱验证码、Token刷新、登出、获取用户信息等接口
 *
 * @param client HTTP客户端实例
 */
class AuthApi(private val client: ApiClient) {
    // JSON解析器，忽略未知字段以兼容后端返回格式变化
    private val json = Json { ignoreUnknownKeys = true }

    /** 管理员登录 */
    suspend fun adminLogin(request: LoginRequest): ApiResponse<LoginResponse> {
        val response = client.post("/auth/admin/login", json.encodeToString(LoginRequest.serializer(), request))
        return json.decodeFromString(ApiResponse.serializer(LoginResponse.serializer()), response)
    }

    /** 普通用户登录 */
    suspend fun userLogin(request: LoginRequest): ApiResponse<LoginResponse> {
        val response = client.post("/auth/user/login", json.encodeToString(LoginRequest.serializer(), request))
        return json.decodeFromString(ApiResponse.serializer(LoginResponse.serializer()), response)
    }

    /** 用户注册 */
    suspend fun register(request: RegisterRequest): ApiResponse<Unit> {
        val response = client.post("/auth/register", json.encodeToString(RegisterRequest.serializer(), request))
        return json.decodeFromString(ApiResponse.serializer(kotlinx.serialization.builtins.serializer<Unit>()), response)
    }

    /**
     * 发送邮箱验证码
     * @param email 目标邮箱地址
     * @param type 验证码类型（如 "register"）
     */
    suspend fun sendEmailCode(email: String, type: String): ApiResponse<Unit> {
        val body = buildJsonObject {
            put("email", JsonPrimitive(email))
            put("type", JsonPrimitive(type))
        }.toString()
        val response = client.post("/auth/email-code", body)
        return json.decodeFromString(ApiResponse.serializer(kotlinx.serialization.builtins.serializer<Unit>()), response)
    }

    /** 刷新访问令牌 */
    suspend fun refreshToken(refreshToken: String): ApiResponse<LoginResponse> {
        val body = buildJsonObject {
            put("refreshToken", JsonPrimitive(refreshToken))
        }.toString()
        val response = client.post("/auth/refresh", body)
        return json.decodeFromString(ApiResponse.serializer(LoginResponse.serializer()), response)
    }

    /**
     * 用户登出
     * @param id 用户ID
     * @param role 用户角色
     */
    suspend fun logout(id: Int, role: String): ApiResponse<Unit> {
        val body = buildJsonObject {
            put("id", JsonPrimitive(id))
            put("role", JsonPrimitive(role))
        }.toString()
        val response = client.post("/auth/logout", body)
        return json.decodeFromString(ApiResponse.serializer(kotlinx.serialization.builtins.serializer<Unit>()), response)
    }

    /** 获取当前登录用户信息 */
    suspend fun getUserInfo(): ApiResponse<User> {
        val response = client.get("/auth/user/info")
        return json.decodeFromString(ApiResponse.serializer(User.serializer()), response)
    }
}
