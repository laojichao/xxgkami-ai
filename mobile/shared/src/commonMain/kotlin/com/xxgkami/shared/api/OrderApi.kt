package com.xxgkami.shared.api

import com.xxgkami.shared.model.*
import kotlinx.serialization.json.Json

/**
 * 订单相关API
 * 封装订单列表查询和创建订单接口
 *
 * @param client HTTP客户端实例
 */
class OrderApi(private val client: ApiClient) {
    // JSON解析器
    private val json = Json { ignoreUnknownKeys = true }

    /** 获取当前用户的订单列表 */
    suspend fun getMyOrders(): ApiResponse<List<Order>> {
        val response = client.get("/orders")
        return json.decodeFromString(ApiResponse.serializer(kotlinx.serialization.builtins.ListSerializer(Order.serializer())), response)
    }

    /**
     * 创建新订单
     * @param data 订单JSON数据
     */
    suspend fun createOrder(data: String): ApiResponse<Order> {
        val response = client.post("/orders", data)
        return json.decodeFromString(ApiResponse.serializer(Order.serializer()), response)
    }
}
