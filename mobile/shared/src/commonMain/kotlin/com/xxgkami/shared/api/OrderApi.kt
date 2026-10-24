package com.xxgkami.shared.api

import com.xxgkami.shared.model.*
import kotlinx.serialization.Serializable

/**
 * 创建订单请求体
 *
 * @property cardType 卡密类型
 * @property cardSpec 卡密规格
 * @property quantity 购买数量
 * @property paymentMethod 支付方式
 * @property pricingId 关联的定价策略ID（后端 @NotNull 必填，缺失必 400）
 */
@Serializable
data class CreateOrderRequest(
    val cardType: String,
    val cardSpec: String,
    val quantity: Int,
    val paymentMethod: String? = null,
    val pricingId: Int
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

    /**
     * 获取当前用户的订单列表。
     *
     * 后端 GET /orders 返回 Page<Order> 分页结构（{content:[...], totalPages, ...}），
     * 此处先按 PageResponse 解析再取 content；未带分页参数时后端默认返回第 0 页（20 条）。
     */
    suspend fun getMyOrders(): ApiResponse<List<Order>> {
        val response = client.get("/orders")
        val page = json.decodeFromString(ApiResponse.serializer(pageSerializer(Order.serializer())), response)
        return page.unwrapPage()
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
