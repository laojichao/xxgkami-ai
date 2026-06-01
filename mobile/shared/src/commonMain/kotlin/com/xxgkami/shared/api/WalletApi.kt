package com.xxgkami.shared.api

import com.xxgkami.shared.model.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * 钱包相关API
 * 封装钱包信息查询、充值、交易记录查询等接口
 *
 * @param client HTTP客户端实例
 */
class WalletApi(private val client: ApiClient) {
    // JSON解析器
    private val json = Json { ignoreUnknownKeys = true }

    /** 获取用户钱包信息 */
    suspend fun getWallet(): ApiResponse<Wallet> {
        val response = client.get("/wallet")
        return json.decodeFromString(ApiResponse.serializer(Wallet.serializer()), response)
    }

    /**
     * 钱包充值
     * @param amount 充值金额
     */
    suspend fun recharge(amount: String): ApiResponse<Wallet> {
        val body = buildJsonObject {
            put("amount", JsonPrimitive(amount))
        }.toString()
        val response = client.post("/wallet/recharge", body)
        return json.decodeFromString(ApiResponse.serializer(Wallet.serializer()), response)
    }

    /** 获取交易记录列表 */
    suspend fun getTransactions(): ApiResponse<List<WalletTransaction>> {
        val response = client.get("/wallet/transactions")
        return json.decodeFromString(ApiResponse.serializer(kotlinx.serialization.builtins.ListSerializer(WalletTransaction.serializer())), response)
    }
}
