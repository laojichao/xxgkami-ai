package com.xxgkami.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xxgkami.shared.api.ApiProvider
import com.xxgkami.shared.api.OrderApi
import com.xxgkami.shared.model.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OrderViewModel : ViewModel() {
    private val apiClient = ApiProvider.apiClient
    private val orderApi = OrderApi(apiClient)

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadOrders() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = orderApi.getMyOrders()
                _orders.value = response.data ?: emptyList()
            } catch (e: Exception) {
                _error.value = e.message ?: "加载订单失败"
            }
            _isLoading.value = false
        }
    }
}
