package com.xxgkami.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xxgkami.shared.api.ApiProvider
import com.xxgkami.shared.api.WalletApi
import com.xxgkami.shared.model.Wallet
import com.xxgkami.shared.model.WalletTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // 错误信息
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /**
     * 加载用户钱包信息
     * 成功时更新 [_wallet]，失败时更新 [_error]
     */
    fun loadWallet() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = walletApi.getWallet()
                _wallet.value = response.data
            } catch (e: Exception) {
                _error.value = e.message ?: "加载钱包失败"
            }
            _isLoading.value = false
        }
    }

    /**
     * 钱包充值
     * @param amount 充值金额（字符串格式）
     */
    fun recharge(amount: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = walletApi.recharge(amount)
                _wallet.value = response.data
            } catch (e: Exception) {
                _error.value = e.message ?: "充值失败"
            }
            _isLoading.value = false
        }
    }

    /**
     * 加载交易记录列表
     * 成功时更新 [_transactions]，失败时更新 [_error]
     */
    fun loadTransactions() {
        viewModelScope.launch {
            try {
                val response = walletApi.getTransactions()
                _transactions.value = response.data ?: emptyList()
            } catch (e: Exception) {
                _error.value = e.message ?: "加载交易记录失败"
            }
        }
    }
}
