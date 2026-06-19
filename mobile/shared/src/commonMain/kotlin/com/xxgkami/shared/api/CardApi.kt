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
     *
     * 注意：此接口返回 [CardVerifyResponse] 而非 [ApiResponse]，
     * 系历史接口设计，服务端直接返回验证结果对象。
     * 为保持向后兼容暂不改动，新接口应统一使用 [ApiResponse] 包装。
     *
     * @param cardKey 卡密字符串
     * @param deviceId 设备标识
     * @param ipAddress IP地址（可选）
     * @return 验证结果，包含成功状态、剩余次数、到期时间等
     */
    suspend fun useCard(cardKey: String, deviceId: String, ipAddress: String = ""): CardVerifyResponse {
        val body = buildJsonObject {
            put("card_key", JsonPrimitive(cardKey))
            put("device_id", JsonPrimitive(deviceId))
            put("ip_address", JsonPrimitive(ipAddress))
        }.toString()
        val response = client.post("/cards/use", body)
        return json.decodeFromString(CardVerifyResponse.serializer(), response)
    }

    /**
     * 获取当前登录用户的卡密列表。
     *
     * 安全说明：使用 `/cards/user/me` 路径，由服务端从 Token 中提取 userId，
     * 避免通过路径参数传递 userId 导致的 IDOR（不安全直接对象引用）漏洞。
     *
     * @return 用户卡密列表响应
     */
    suspend fun getUserCards(): ApiResponse<List<Card>> {
        val response = client.get("/cards/user/me")
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

    /**
     * 解绑卡密的机器码。
     *
     * 合并了原有的两个重载函数，通过 [machineCode] 参数区分行为：
     * - 当 [machineCode] 非空时，走"需机器码验证"的解绑流程
     * - 当 [machineCode] 为 null 时，走"自助解绑"流程（无需机器码验证）
     *
     * @param cardKey 卡密字符串
     * @param machineCode 要解绑的机器码，null 表示自助解绑（不验证机器码）
     * @return 解绑结果响应
     */
    suspend fun machineUnbind(cardKey: String, machineCode: String? = null): ApiResponse<Unit> {
        val body = buildJsonObject {
            put("card_key", JsonPrimitive(cardKey))
            // machineCode 为 null 时不传 machine_code 字段，服务端走自助解绑流程
            if (machineCode != null) {
                put("machine_code", JsonPrimitive(machineCode))
            }
        }.toString()
        val response = client.post("/public/cards/machine-bind/unbind", body)
        return json.decodeFromString(ApiResponse.serializer(kotlinx.serialization.builtins.serializer<Unit>()), response)
    }
}
