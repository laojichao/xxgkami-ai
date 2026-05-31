package com.xxgkami.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xxgkami.android.data.TokenStore
import com.xxgkami.shared.api.ApiProvider
import com.xxgkami.shared.api.AuthApi
import com.xxgkami.shared.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val apiClient = ApiProvider.apiClient
    private val authApi = AuthApi(apiClient)

    private val _loginState = MutableStateFlow<ApiResponse<LoginResponse>?>(null)
    val loginState: StateFlow<ApiResponse<LoginResponse>?> = _loginState

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _userInfo = MutableStateFlow<User?>(null)
    val userInfo: StateFlow<User?> = _userInfo

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _registerSuccess = MutableStateFlow(false)
    val registerSuccess: StateFlow<Boolean> = _registerSuccess

    init {
        // Token callbacks are managed by TokenStore to avoid conflicts
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _loginState.value = authApi.userLogin(LoginRequest(username, password))
                handleLoginSuccess(_loginState.value)
            } catch (e: Exception) {
                _loginState.value = ApiResponse(false, e.message)
            }
            _isLoading.value = false
        }
    }

    fun adminLogin(username: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _loginState.value = authApi.adminLogin(LoginRequest(username, password))
                handleLoginSuccess(_loginState.value)
            } catch (e: Exception) {
                _loginState.value = ApiResponse(false, e.message)
            }
            _isLoading.value = false
        }
    }

    private fun handleLoginSuccess(response: ApiResponse<LoginResponse>?) {
        response?.data?.let { data ->
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

    fun register(username: String, password: String, email: String, code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _registerSuccess.value = false
            try {
                authApi.register(RegisterRequest(username, password, email, code))
                _registerSuccess.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "注册失败"
            }
            _isLoading.value = false
        }
    }

    fun sendCode(email: String, type: String = "register") {
        viewModelScope.launch {
            _error.value = null
            try {
                authApi.sendEmailCode(email, type)
            } catch (e: Exception) {
                _error.value = e.message ?: "发送验证码失败"
            }
        }
    }

    fun getUserInfo() {
        viewModelScope.launch {
            try {
                val response = authApi.getUserInfo()
                _userInfo.value = response.data
            } catch (e: Exception) {
                _error.value = e.message ?: "获取用户信息失败"
            }
        }
    }

    fun logout(userId: Int, role: String) {
        viewModelScope.launch {
            try { authApi.logout(userId, role) } catch (_: Exception) {}
            TokenStore.clearTokens()
            _loginState.value = null
            _userInfo.value = null
        }
    }

    fun clearError() {
        _error.value = null
    }
}
