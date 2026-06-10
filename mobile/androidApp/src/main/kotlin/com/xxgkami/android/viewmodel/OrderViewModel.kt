package com.xxgkami.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xxgkami.shared.api.ApiProvider
import com.xxgkami.shared.api.OrderApi
import com.xxgkami.shared.model.Order
import com.xxgkami.android.util.ErrorMapper
import kotlinx.coroutines.CancellationException
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

    // 数据缓存标记：避免页面切换时重复请求
    private var ordersLoaded = false

    /**
     * 加载当前用户的订单列表
     * @param forceRefresh 是否强制刷新，忽略缓存
     * 成功时更新 [_orders]，失败时更新 [_error]
     */
    fun loadOrders(forceRefresh: Boolean = false) {
        if (ordersLoaded && !forceRefresh) return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = orderApi.getMyOrders()
                _orders.value = response.data ?: emptyList()
                ordersLoaded = true
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
     * 重置缓存标记
     * 在用户登出时调用，确保下次登录后重新加载数据
     */
    fun resetCache() {
        ordersLoaded = false
    }
}
