package com.xxgkami.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xxgkami.shared.api.ApiProvider
import com.xxgkami.shared.api.CardApi
import com.xxgkami.shared.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CardViewModel : ViewModel() {
    private val apiClient = ApiProvider.apiClient
    private val cardApi = CardApi(apiClient)

    private val _verifyResult = MutableStateFlow<CardVerifyResponse?>(null)
    val verifyResult: StateFlow<CardVerifyResponse?> = _verifyResult

    private val _cards = MutableStateFlow<List<Card>>(emptyList())
    val cards: StateFlow<List<Card>> = _cards

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun verify(cardKey: String, deviceId: String = "Android") {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _verifyResult.value = cardApi.useCard(cardKey, deviceId)
            } catch (e: Exception) {
                _error.value = e.message ?: "验证失败"
                _verifyResult.value = CardVerifyResponse(false, e.message)
            }
            _isLoading.value = false
        }
    }

    fun loadUserCards(userId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = cardApi.getUserCards(userId)
                _cards.value = response.data ?: emptyList()
            } catch (e: Exception) {
                _error.value = e.message ?: "加载卡密失败"
            }
            _isLoading.value = false
        }
    }

    fun unbindMachineCode(cardKey: String, machineCode: String) {
        viewModelScope.launch {
            _error.value = null
            try {
                cardApi.machineUnbind(cardKey, machineCode)
            } catch (e: Exception) {
                _error.value = e.message ?: "解绑失败"
            }
        }
    }
}
