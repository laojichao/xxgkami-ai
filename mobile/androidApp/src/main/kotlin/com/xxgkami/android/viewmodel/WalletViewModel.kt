package com.xxgkami.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xxgkami.shared.api.ApiProvider
import com.xxgkami.shared.api.WalletApi
import com.xxgkami.shared.model.Wallet
import com.xxgkami.shared.model.WalletTransaction
import com.xxgkami.android.util.ErrorMapper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 钱包业务ViewModel
 * 管理用户钱包余额、充值操作、交易记录的加载和状态
 * 通过 StateFlow 暴露钱包数据给 UI 层
 */
class WalletViewModel : ViewModel() {
    // API客户端和钱包API实例
    private val apiClient = ApiProvider.apiClient
    private val walletApi = WalletApi(apiClient)

    // 钱包信息（余额、累计充值、累计消费）
    private val _wallet = MutableStateFlow<Wallet?>(null)
    val wallet: StateFlow<Wallet?> = _wallet

    // 交易记录列表
    private val _transactions = MutableStateFlow<List<WalletTransaction>>(emptyList())
    val transactions: StateFlow<List<WalletTransaction>> = _transactions

    // 加载状态（使用计数器支持并发操作，如同时加载钱包和交易记录）
    private val _loadingCount = MutableStateFlow(0)
    val isLoading: StateFlow<Boolean> = _loadingCount.map { it > 0 }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), false)

    // 错误信息
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // 数据缓存标记：避免页面切换时重复请求
    private var walletLoaded = false
    private var transactionsLoaded = false

    /**
     * 加载用户钱包信息
     * @param forceRefresh 是否强制刷新，忽略缓存
     * 成功时更新 [_wallet]，失败时更新 [_error]
     */
    fun loadWallet(forceRefresh: Boolean = false) {
        if (walletLoaded && !forceRefresh) return
        viewModelScope.launch {
            _loadingCount.value++
            _error.value = null
            try {
                val response = walletApi.getWallet()
                _wallet.value = response.data
                walletLoaded = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = ErrorMapper.mapError(e)
            } finally {
                _loadingCount.value--
            }
        }
    }

    /**
     * 钱包充值
     * @param amount 充值金额（字符串格式）
     */
    fun recharge(amount: String) {
        // 客户端校验充值金额
        val parsedAmount = amount.toDoubleOrNull()
        if (parsedAmount == null || parsedAmount <= 0) {
            _error.value = "请输入有效的充值金额"
            return
        }
        viewModelScope.launch {
            _loadingCount.value++
            _error.value = null
            try {
                val response = walletApi.recharge(amount)
                _wallet.value = response.data
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = ErrorMapper.mapError(e)
            } finally {
                _loadingCount.value--
            }
        }
    }

    /**
     * 加载交易记录列表
     * @param forceRefresh 是否强制刷新，忽略缓存
     * 成功时更新 [_transactions]，失败时更新 [_error]
     */
    fun loadTransactions(forceRefresh: Boolean = false) {
        if (transactionsLoaded && !forceRefresh) return
        viewModelScope.launch {
            _loadingCount.value++
            _error.value = null
            try {
                val response = walletApi.getTransactions()
                _transactions.value = response.data ?: emptyList()
                transactionsLoaded = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = ErrorMapper.mapError(e)
            } finally {
                _loadingCount.value--
            }
        }
    }

    /**
     * 重置所有缓存标记
     * 在用户登出时调用，确保下次登录后重新加载数据
     */
    fun resetCache() {
        walletLoaded = false
        transactionsLoaded = false
    }
}
