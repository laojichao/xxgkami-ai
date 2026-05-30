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

    fun verify(cardKey: String, deviceId: String = "Android") {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _verifyResult.value = cardApi.useCard(cardKey, deviceId)
            } catch (e: Exception) {
                _verifyResult.value = CardVerifyResponse(false, e.message)
            }
            _isLoading.value = false
        }
    }

    fun loadUserCards(userId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = cardApi.getUserCards(userId)
                _cards.value = response.data ?: emptyList()
            } catch (_: Exception) {}
            _isLoading.value = false
        }
    }

    fun unbindMachineCode(cardKey: String) {
        viewModelScope.launch {
            try { cardApi.machineUnbind(cardKey) } catch (_: Exception) {}
        }
    }
}
