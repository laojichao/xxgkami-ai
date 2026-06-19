package com.xxgkami.shared.api

import com.xxgkami.shared.model.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 创建订单请求体
 *
 * @property cardType 卡密类型
 * @property cardSpec 卡密规格
 * @property quantity 购买数量
 * @property paymentMethod 支付方式
 */
@Serializable
data class CreateOrderRequest(
    val cardType: String,
    val cardSpec: String,
    val quantity: Int,
    val paymentMethod: String? = null
)

/**
 * 订单相关API
 * 封装订单列表查询和创建订单接口
 *
 * @param client HTTP客户端实例
 */
class OrderApi(private val client: ApiClient) {
    // 使用全局共享的 JSON 解析器，保持配置一致性
    private val json = ApiProvider.json

    /** 获取当前用户的订单列表 */
    suspend fun getMyOrders(): ApiResponse<List<Order>> {
        val response = client.get("/orders")
        return json.decodeFromString(ApiResponse.serializer(kotlinx.serialization.builtins.ListSerializer(Order.serializer())), response)
    }

    /**
     * 创建新订单
     * @param request 订单请求数据
     */
    suspend fun createOrder(request: CreateOrderRequest): ApiResponse<Order> {
        val body = json.encodeToString(CreateOrderRequest.serializer(), request)
        val response = client.post("/orders", body)
        return json.decodeFromString(ApiResponse.serializer(Order.serializer()), response)
    }
}
