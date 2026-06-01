package com.xxgkami.shared.util

/**
 * 内存Token管理器
 * 在应用运行期间保存用户的认证信息（令牌、用户名、角色）
 * 注意：数据仅保存在内存中，应用进程结束后将丢失
 */
class TokenManager {
    var accessToken: String? = null   // 访问令牌
    var refreshToken: String? = null  // 刷新令牌
    var username: String? = null      // 用户名
    var role: String? = null          // 用户角色

    /** 判断用户是否已登录（通过访问令牌是否存在） */
    fun isLoggedIn(): Boolean = accessToken != null

    /** 判断当前用户是否为管理员 */
    fun isAdmin(): Boolean = role == "admin"

    /** 清除所有认证信息 */
    fun clear() {
        accessToken = null
        refreshToken = null
        username = null
        role = null
    }
}
