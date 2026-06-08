package com.xxgkami.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xxgkami.shared.api.ApiProvider
import com.xxgkami.shared.api.CardApi
import com.xxgkami.shared.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 卡密业务ViewModel
 * 管理卡密验证、用户卡密列表加载、机器码解绑等状态
 * 通过 StateFlow 暴露状态给 UI 层观察
 */
class CardViewModel : ViewModel() {
    // API客户端和卡密API实例
    private val apiClient = ApiProvider.apiClient
    private val cardApi = CardApi(apiClient)

    // 卡密验证结果
    private val _verifyResult = MutableStateFlow<CardVerifyResponse?>(null)
    val verifyResult: StateFlow<CardVerifyResponse?> = _verifyResult

    // 用户卡密列表
    private val _cards = MutableStateFlow<List<Card>>(emptyList())
    val cards: StateFlow<List<Card>> = _cards

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // 错误信息
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /**
     * 验证卡密
     * @param cardKey 卡密字符串
     * @param deviceId 设备标识，默认为"Android"
     */
    fun verify(cardKey: String, deviceId: String = "Android") {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _verifyResult.value = cardApi.useCard(cardKey, deviceId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = e.message ?: "验证失败"
                _verifyResult.value = CardVerifyResponse(false, e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 加载指定用户的卡密列表
     * @param userId 用户ID
     */
    fun loadUserCards(userId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = cardApi.getUserCards(userId)
                _cards.value = response.data ?: emptyList()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = e.message ?: "加载卡密失败"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 解绑卡密的机器码
     * @param cardKey 卡密字符串
     * @param machineCode 要解绑的机器码
     */
    fun unbindMachineCode(cardKey: String, machineCode: String) {
        viewModelScope.launch {
            _error.value = null
            try {
                cardApi.machineUnbind(cardKey, machineCode)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = e.message ?: "解绑失败"
            }
        }
    }
}
