package com.xxgkami.android.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.xxgkami.shared.api.ApiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object TokenStore {
    private const val TAG = "TokenStore"
    private const val PREFS_NAME = "xxgkami_auth_encrypted"

    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_USERNAME = "username"
    private const val KEY_ROLE = "role"

    @Volatile
    private var prefs: SharedPreferences? = null

    /**
     * 初始化 TokenStore，必须在 IO 线程调用以避免阻塞主线程。
     *
     * EncryptedSharedPreferences 的创建涉及密钥派生和文件 I/O，
     * 在主线程执行会导致 ANR。使用 suspend 函数强制调用方在协程中调用。
     *
     * @param context 应用上下文
     */
    suspend fun init(context: Context) {
        val appContext = context.applicationContext
        // 在 IO 线程执行加密存储初始化，避免阻塞主线程
        val initializedPrefs = withContext(Dispatchers.IO) {
            // 使用 EncryptedSharedPreferences 加密存储 Token，防止 root 设备提取明文
            // 添加 try-catch 降级处理：部分设备（三星/华为等）可能出现 GeneralSecurityException
            try {
                val masterKey = MasterKey.Builder(appContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                EncryptedSharedPreferences.create(
                    appContext,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                Log.w(TAG, "EncryptedSharedPreferences init failed, attempting recovery", e)
                // Try to delete corrupted master key and retry
                try {
                    appContext.deleteSharedPreferences(PREFS_NAME)
                    // After deleting, recreate directly with encryption (no intermediate plain SharedPreferences)
                    val masterKey = MasterKey.Builder(appContext)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build()
                    EncryptedSharedPreferences.create(
                        appContext,
                        PREFS_NAME,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    )
                } catch (e2: Exception) {
                    Log.e(TAG, "EncryptedSharedPreferences recovery failed", e2)
                    throw RuntimeException("无法初始化安全存储，请重新登录", e2)
                }
            }
        }
        prefs = initializedPrefs

        // Restore tokens from storage
        val accessToken = prefs?.getString(KEY_ACCESS_TOKEN, null)
        val refreshToken = prefs?.getString(KEY_REFRESH_TOKEN, null)
        if (accessToken != null) {
            ApiProvider.setTokens(accessToken, refreshToken)
        }

        // Save tokens when refreshed
        ApiProvider.setOnTokenRefreshed { newToken, newRefreshToken ->
            saveTokens(newToken, newRefreshToken)
        }

        // Clear tokens on logout
        ApiProvider.setOnLogout {
            clearTokens()
        }
    }

    fun saveTokens(accessToken: String, refreshToken: String) {
        prefs?.edit()?.apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            apply()
        }
        ApiProvider.setTokens(accessToken, refreshToken)
    }

    fun saveUserInfo(username: String, role: String) {
        prefs?.edit()?.apply {
            putString(KEY_USERNAME, username)
            putString(KEY_ROLE, role)
            apply()
        }
    }

    fun getAccessToken(): String? = prefs?.getString(KEY_ACCESS_TOKEN, null)
    fun getRefreshToken(): String? = prefs?.getString(KEY_REFRESH_TOKEN, null)
    fun getUsername(): String? = prefs?.getString(KEY_USERNAME, null)
    fun getRole(): String? = prefs?.getString(KEY_ROLE, null)
    fun isLoggedIn(): Boolean = getAccessToken() != null

    fun clearTokens() {
        prefs?.edit()?.apply {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_USERNAME)
            remove(KEY_ROLE)
            apply()
        }
        ApiProvider.clearTokens()
    }
}
