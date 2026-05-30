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

    fun loadOrders(userId: Int) {
        viewModelScope.launch {
            try {
                val response = orderApi.getMyOrders(userId)
                _orders.value = response.data ?: emptyList()
            } catch (_: Exception) {}
        }
    }
}
