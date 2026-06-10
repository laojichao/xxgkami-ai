package com.xxgkami.shared.api

import com.xxgkami.shared.model.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * 卡密相关API
 * 封装卡密使用/验证、查询用户卡密、机器码绑定查询和解绑等接口
 *
 * @param client HTTP客户端实例
 */
class CardApi(private val client: ApiClient) {
    // 使用全局共享的 JSON 解析器，保持配置一致性
    private val json = ApiProvider.json

    /**
     * 使用/验证卡密
     * @param cardKey 卡密字符串
     * @param deviceId 设备标识
     * @param ipAddress IP地址（可选）
     * @return 验证结果，包含成功状态、剩余次数、到期时间等
     */
    suspend fun useCard(cardKey: String, deviceId: String = "Unknown", ipAddress: String = ""): CardVerifyResponse {
        val body = buildJsonObject {
            put("card_key", JsonPrimitive(cardKey))
            put("device_id", JsonPrimitive(deviceId))
            put("ip_address", JsonPrimitive(ipAddress))
        }.toString()
        val response = client.post("/cards/use", body)
        return json.decodeFromString(CardVerifyResponse.serializer(), response)
    }

    /** 获取指定用户的卡密列表 */
    suspend fun getUserCards(userId: Int): ApiResponse<List<Card>> {
        val response = client.get("/cards/user/$userId")
        return json.decodeFromString(ApiResponse.serializer(kotlinx.serialization.builtins.ListSerializer(Card.serializer())), response)
    }

    /** 查询卡密的机器码绑定信息 */
    suspend fun machineBindQuery(cardKey: String): ApiResponse<Map<String, String>> {
        val body = buildJsonObject {
            put("card_key", JsonPrimitive(cardKey))
        }.toString()
        val response = client.post("/public/cards/machine-bind/query", body)
        return json.decodeFromString(ApiResponse.serializer(kotlinx.serialization.builtins.MapSerializer(kotlinx.serialization.builtins.serializer<String>(), kotlinx.serialization.builtins.serializer<String>())), response)
    }

    /** 解绑卡密的机器码 */
    suspend fun machineUnbind(cardKey: String, machineCode: String): ApiResponse<Unit> {
        val body = buildJsonObject {
            put("card_key", JsonPrimitive(cardKey))
            put("machine_code", JsonPrimitive(machineCode))
        }.toString()
        val response = client.post("/public/cards/machine-bind/unbind", body)
        return json.decodeFromString(ApiResponse.serializer(kotlinx.serialization.builtins.serializer<Unit>()), response)
    }
}
