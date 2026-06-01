package com.xxgkami.shared.model

import kotlinx.serialization.Serializable

/**
 * 用户数据模型
 * 表示系统中的用户信息
 *
 * @property id 用户ID
 * @property username 用户名
 * @property email 邮箱
 * @property nickname 昵称
 * @property avatar 头像URL
 * @property phone 手机号
 * @property status 账号状态（true: 正常, false: 禁用）
 * @property role 角色（admin: 管理员, user: 普通用户）
 * @property createTime 注册时间
 */
@Serializable
data class User(
    val id: Int? = null,
    val username: String? = null,
    val email: String? = null,
    val nickname: String? = null,
    val avatar: String? = null,
    val phone: String? = null,
    val status: Boolean? = null,
    val role: String? = null,
    val createTime: String? = null
)

/** 登录请求 */
@Serializable
data class LoginRequest(val username: String, val password: String)

/** 注册请求 */
@Serializable
data class RegisterRequest(val username: String, val password: String, val email: String, val code: String)

/**
 * 登录响应
 * @property token 访问令牌
 * @property refreshToken 刷新令牌
 * @property userInfo 登录成功后的用户信息
 */
@Serializable
data class LoginResponse(
    val token: String? = null,
    val refreshToken: String? = null,
    val userInfo: User? = null
)
