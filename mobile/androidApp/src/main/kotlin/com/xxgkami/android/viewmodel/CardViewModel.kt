package com.xxgkami.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xxgkami.shared.api.ApiProvider
import com.xxgkami.shared.api.CardApi
import com.xxgkami.shared.model.*
import com.xxgkami.android.util.ErrorMapper
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

    // 解绑加载状态，独立于 [_isLoading]，避免与列表加载状态冲突
    private val _unbindLoading = MutableStateFlow(false)
    val unbindLoading: StateFlow<Boolean> = _unbindLoading

    // 解绑结果反馈，UI 据此显示成功/失败提示
    private val _unbindResult = MutableStateFlow<String?>(null)
    val unbindResult: StateFlow<String?> = _unbindResult

    // 数据缓存标记：避免页面切换时重复请求
    private var cardsLoaded = false

    /**
     * 验证卡密
     * @param cardKey 卡密字符串
     * @param deviceId 设备标识，默认为"Android"
     */
    fun verify(cardKey: String, deviceId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _verifyResult.value = cardApi.useCard(cardKey, deviceId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 复用已映射的错误信息，避免重复调用 ErrorMapper.mapError
                val errorMsg = ErrorMapper.mapError(e)
                _error.value = errorMsg
                _verifyResult.value = CardVerifyResponse(false, errorMsg)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 加载当前登录用户的卡密列表。
     *
     * 安全说明：userId 由服务端从 Token 中提取，客户端不再传递 userId 参数，
     * 避免 IDOR 漏洞。
     *
     * @param forceRefresh 是否强制刷新，忽略缓存
     */
    fun loadUserCards(forceRefresh: Boolean = false) {
        if (cardsLoaded && !forceRefresh) return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = cardApi.getUserCards()
                _cards.value = response.data ?: emptyList()
                cardsLoaded = true
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
     * 解绑卡密的机器码（需提供机器码验证）
     * @param cardKey 卡密字符串
     * @param machineCode 要解绑的机器码
     */
    fun unbindMachineCode(cardKey: String, machineCode: String) {
        unbindMachineCodeInternal(cardKey, machineCode)
    }

    /**
     * 自助解绑卡密的机器码（无需机器码验证）
     * @param cardKey 卡密字符串
     */
    fun unbindMachineCode(cardKey: String) {
        unbindMachineCodeInternal(cardKey, null)
    }

    /**
     * 解绑机器码的内部实现，统一处理加载状态和结果反馈。
     *
     * @param cardKey 卡密字符串
     * @param machineCode 机器码，null 表示自助解绑
     */
    private fun unbindMachineCodeInternal(cardKey: String, machineCode: String?) {
        viewModelScope.launch {
            _unbindLoading.value = true
            _unbindResult.value = null
            _error.value = null
            try {
                val response = cardApi.machineUnbind(cardKey, machineCode)
                if (response.success) {
                    _unbindResult.value = response.message ?: "解绑成功"
                    // 解绑成功后刷新卡密列表
                    loadUserCards(forceRefresh = true)
                } else {
                    _unbindResult.value = response.message ?: "解绑失败"
                    _error.value = response.message
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val errorMsg = ErrorMapper.mapError(e)
                _unbindResult.value = errorMsg
                _error.value = errorMsg
            } finally {
                _unbindLoading.value = false
            }
        }
    }

    /**
     * 清除解绑结果反馈，UI 展示完提示后应调用此方法。
     */
    fun clearUnbindResult() {
        _unbindResult.value = null
    }

    /**
     * 重置缓存标记
     * 在用户登出时调用，确保下次登录后重新加载数据
     */
    fun resetCache() {
        cardsLoaded = false
        _cards.value = emptyList()
        _verifyResult.value = null
        _error.value = null
        _unbindResult.value = null
        _unbindLoading.value = false
    }
}
