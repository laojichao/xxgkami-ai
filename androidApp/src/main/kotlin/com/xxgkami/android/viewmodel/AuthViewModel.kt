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

    init {
        // Set up token refresh callback
        ApiProvider.setOnTokenRefreshed { newToken, newRefreshToken ->
            // Token refreshed automatically by ApiClient
        }

        // Set up logout callback (called when refresh fails)
        ApiProvider.setOnLogout {
            _loginState.value = null
            _userInfo.value = null
        }
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
            try {
                authApi.register(RegisterRequest(username, password, email, code))
            } catch (_: Exception) { }
            _isLoading.value = false
        }
    }

    fun sendCode(email: String, type: String = "register") {
        viewModelScope.launch {
            try { authApi.sendEmailCode(email, type) } catch (_: Exception) {}
        }
    }

    fun getUserInfo() {
        viewModelScope.launch {
            try {
                val response = authApi.getUserInfo()
                _userInfo.value = response.data
            } catch (_: Exception) {}
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
}
