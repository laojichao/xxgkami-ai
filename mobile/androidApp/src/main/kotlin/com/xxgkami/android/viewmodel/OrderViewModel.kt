package com.xxgkami.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xxgkami.shared.api.ApiProvider
import com.xxgkami.shared.api.OrderApi
import com.xxgkami.shared.model.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 订单业务ViewModel
 * 管理订单列表的加载和错误状态
 * 通过 StateFlow 暴露订单数据给 UI 层
 */
class OrderViewModel : ViewModel() {
    // API客户端和订单API实例
    private val apiClient = ApiProvider.apiClient
    private val orderApi = OrderApi(apiClient)

    // 订单列表数据
    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // 错误信息
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /**
     * 加载当前用户的订单列表
     * 成功时更新 [_orders]，失败时更新 [_error]
     */
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
