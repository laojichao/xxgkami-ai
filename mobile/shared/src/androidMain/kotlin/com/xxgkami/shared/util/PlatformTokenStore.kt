package com.xxgkami.shared.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Android平台Token持久化 actual实现
 *
 * 自包含实现（基于 SharedPreferences），不得反向 import androidApp 模块的 TokenStore，
 * 否则会形成 shared -> androidApp 的循环依赖，导致 Android 端编译失败。
 *
 * 现状说明：androidApp 实际使用其自身模块内基于 EncryptedSharedPreferences 的
 * TokenStore（支持密钥派生失败降级与用户信息存储），本类当前无调用方，
 * 仅用于满足 expect/actual 声明，保留供后续统一跨平台存储时启用。
 * 若要启用请先调用 [init] 注入应用上下文；未注入 Context 时各方法为空操作/返回 null。
 */
actual class PlatformTokenStore actual constructor() {

    actual fun saveTokens(accessToken: String, refreshToken: String) {
        prefs()?.edit()?.apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            apply()
        }
    }

    actual fun getAccessToken(): String? = prefs()?.getString(KEY_ACCESS_TOKEN, null)

    actual fun getRefreshToken(): String? = prefs()?.getString(KEY_REFRESH_TOKEN, null)

    actual fun clearTokens() {
        prefs()?.edit()?.apply {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            apply()
        }
    }

    actual fun isLoggedIn(): Boolean = getAccessToken() != null

    companion object {
        private const val PREFS_NAME = "xxgkami_platform_token_store"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"

        @Volatile
        private var appContext: Context? = null

        /**
         * 注入应用上下文，须在 Application/Activity 启动时调用一次。
         * @param context 任意 Context，内部取 applicationContext 防止泄漏
         */
        fun init(context: Context) {
            appContext = context.applicationContext
        }

        private fun prefs(): SharedPreferences? =
            appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
