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

class WalletViewModel : ViewModel() {
    private val apiClient = ApiProvider.apiClient
    private val walletApi = WalletApi(apiClient)

    private val _wallet = MutableStateFlow<Wallet?>(null)
    val wallet: StateFlow<Wallet?> = _wallet

    private val _transactions = MutableStateFlow<List<WalletTransaction>>(emptyList())
    val transactions: StateFlow<List<WalletTransaction>> = _transactions

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

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
