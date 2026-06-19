package com.xxgkami.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xxgkami.android.data.TokenStore
import com.xxgkami.android.util.ErrorMapper
import com.xxgkami.shared.api.ApiProvider
import com.xxgkami.shared.api.AuthApi
import com.xxgkami.shared.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * 认证相关 ViewModel，管理用户登录、注册、登出等认证状态。
 *
 * 通过 [StateFlow] 暴露 UI 状态（加载中、登录结果、用户信息、错误信息），
 * 供 Compose 或 XML 界面层观察和响应。
 *
 * Token 的持久化存储由 [TokenStore] 统一管理，避免与 ApiClient 回调冲突。
 *
 * 客户端限流说明：[failedLoginAttempts] 和 [lastFailedTimestamp] 使用原子类型保证线程安全，
 * 但客户端限流仅为辅助措施，服务端应做最终限流和账户锁定。
 */
class AuthViewModel : ViewModel() {
    private val apiClient = ApiProvider.apiClient
    private val authApi = AuthApi(apiClient)

    /** 客户端登录限流：最大失败尝试次数 */
    private companion object {
        const val MAX_LOGIN_ATTEMPTS = 5
        const val COOLDOWN_MILLIS = 60_000L // 60 秒冷却期
    }

    /** 登录失败尝试记录（使用原子类型保证线程安全，避免多协程并发更新丢失） */
    private val failedLoginAttempts = AtomicInteger(0)
    private val lastFailedTimestamp = AtomicLong(0L)

    /** 登录响应状态，包含成功/失败信息及登录数据 */
    private val _loginState = MutableStateFlow<ApiResponse<LoginResponse>?>(null)
    val loginState: StateFlow<ApiResponse<LoginResponse>?> = _loginState

    /** 是否正在加载中，用于 UI 显示加载指示器 */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    /** 当前登录用户信息，登出或未登录时为 null */
    private val _userInfo = MutableStateFlow<User?>(null)
    val userInfo: StateFlow<User?> = _userInfo

    /** 错误信息，用于 UI 展示提示；null 表示无错误 */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /** 注册是否成功，UI 观察此值以跳转登录页或显示成功提示 */
    private val _registerSuccess = MutableStateFlow(false)
    val registerSuccess: StateFlow<Boolean> = _registerSuccess

    /** 用户信息缓存标记：避免页面切换时重复请求 */
    private var userInfoLoaded = false

    init {
        // Token 回调由 TokenStore 统一管理，避免与 ApiClient 的 onTokenRefreshed/onLogout 冲突
    }

    /**
     * 用户登录。
     *
     * 成功后自动保存 Token 和用户基本信息到 [TokenStore]。
     *
     * @param username 用户名
     * @param password 密码
     */
    fun login(username: String, password: String) {
        // 客户端限流：检查冷却期
        val attempts = failedLoginAttempts.get()
        if (attempts >= MAX_LOGIN_ATTEMPTS) {
            val elapsed = System.currentTimeMillis() - lastFailedTimestamp.get()
            if (elapsed < COOLDOWN_MILLIS) {
                val remainingSeconds = ((COOLDOWN_MILLIS - elapsed) / 1000).toInt()
                _loginState.value = ApiResponse(false, "登录尝试过于频繁，请${remainingSeconds}秒后重试")
                return
            }
            // 冷却期已过，重置计数
            failedLoginAttempts.set(0)
        }

        viewModelScope.launch {
            _loginState.value = null
            _error.value = null
            _isLoading.value = true
            try {
                _loginState.value = authApi.userLogin(LoginRequest(username, password))
                val success = _loginState.value?.success == true
                if (success) {
                    failedLoginAttempts.set(0) // 登录成功，重置计数
                    handleLoginSuccess(_loginState.value)
                } else {
                    recordLoginFailure()
                }
            } catch (e: CancellationException) {
                throw e // 协程取消异常必须向上传播，不能吞掉
            } catch (e: Exception) {
                _loginState.value = ApiResponse(false, ErrorMapper.mapError(e))
                recordLoginFailure()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 管理员登录。
     *
     * 与普通用户登录共用 [handleLoginSuccess] 处理 Token 持久化。
     *
     * @param username 管理员用户名
     * @param password 管理员密码
     */
    fun adminLogin(username: String, password: String) {
        // 客户端限流：检查冷却期
        val attempts = failedLoginAttempts.get()
        if (attempts >= MAX_LOGIN_ATTEMPTS) {
            val elapsed = System.currentTimeMillis() - lastFailedTimestamp.get()
            if (elapsed < COOLDOWN_MILLIS) {
                val remainingSeconds = ((COOLDOWN_MILLIS - elapsed) / 1000).toInt()
                _loginState.value = ApiResponse(false, "登录尝试过于频繁，请${remainingSeconds}秒后重试")
                return
            }
            failedLoginAttempts.set(0)
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                _loginState.value = authApi.adminLogin(LoginRequest(username, password))
                val success = _loginState.value?.success == true
                if (success) {
                    failedLoginAttempts.set(0)
                    handleLoginSuccess(_loginState.value)
                } else {
                    recordLoginFailure()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _loginState.value = ApiResponse(false, ErrorMapper.mapError(e))
                recordLoginFailure()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** 记录一次登录失败，更新计数器和时间戳（线程安全） */
    private fun recordLoginFailure() {
        failedLoginAttempts.incrementAndGet()
        lastFailedTimestamp.set(System.currentTimeMillis())
    }

    private fun handleLoginSuccess(response: ApiResponse<LoginResponse>?) {
        if (response?.success != true) return
        response.data?.let { data ->
            if (data.token != null && data.refreshToken != null) {
                TokenStore.saveTokens(data.token, data.refreshToken)
            }
            data.userInfo?.let { user ->
                TokenStore.saveUserInfo(
                    user.username ?: "",
                    user.role ?: "user"
                )
            }
        }
    }

    /**
     * 用户注册。
     *
     * 注册成功后设置 [_registerSuccess] 为 true，UI 层可据此跳转至登录页。
     *
     * @param username 用户名
     * @param password 密码
     * @param email 邮箱地址
     * @param code 邮箱验证码
     */
    fun register(username: String, password: String, email: String, code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _registerSuccess.value = false
            try {
                authApi.register(RegisterRequest(username, password, email, code))
                _registerSuccess.value = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = ErrorMapper.mapError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 发送邮箱验证码。
     *
     * @param email 目标邮箱地址
     * @param type 验证码类型，默认 "register"（注册），也可为 "reset"（重置密码）
     */
    fun sendCode(email: String, type: String = "register") {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                authApi.sendEmailCode(email, type)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = ErrorMapper.mapError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 获取当前登录用户信息，更新 [_userInfo] 状态。
     * @param forceRefresh 是否强制刷新，忽略缓存
     */
    fun getUserInfo(forceRefresh: Boolean = false) {
        if (userInfoLoaded && !forceRefresh) return
        viewModelScope.launch {
            _error.value = null
            _isLoading.value = true
            try {
                val response = authApi.getUserInfo()
                _userInfo.value = response.data
                userInfoLoaded = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = ErrorMapper.mapError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 用户登出。
     *
     * 先调用服务端登出接口（携带当前 Token），无论成功失败后清除本地 Token 和用户状态。
     * 这样可避免本地 Token 先被清除导致服务端登出请求 401。
     *
     * @param userId 用户 ID
     * @param role 用户角色（"user" 或 "admin"）
     */
    fun logout(userId: Int, role: String) {
        viewModelScope.launch {
            // 先调用服务端登出接口（携带当前 Token），失败不阻塞本地清理
            try {
                authApi.logout(userId, role)
            } catch (e: CancellationException) {
                // 协程取消异常必须向上传播，不能吞掉
                throw e
            } catch (_: Exception) {
                // 服务端登出失败（如网络异常、401 等）忽略，继续清理本地状态
            } finally {
                // 无论服务端登出成功与否，都清除本地 Token 和状态
                TokenStore.clearTokens()
                _loginState.value = null
                _userInfo.value = null
                _registerSuccess.value = false
                _error.value = null
                userInfoLoaded = false
            }
        }
    }

    /**
     * 重置用户信息缓存标记
     * 供外部在登出流程中调用
     */
    fun resetCache() {
        userInfoLoaded = false
    }

    /**
     * 清除当前错误状态，UI 层在展示错误提示后应调用此方法。
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * 消费登录状态，防止 LoginScreen 重复触发 onLoginSuccess。
     * 在 LaunchedEffect 处理完登录成功后调用。
     */
    fun consumeLoginState() {
        _loginState.value = null
    }
}
