package com.xxgkami.shared.api

import com.xxgkami.shared.model.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

class WalletApi(private val client: ApiClient) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getWallet(): ApiResponse<Wallet> {
        val response = client.get("/wallet")
        return json.decodeFromString(ApiResponse.serializer(Wallet.serializer()), response)
    }

    suspend fun recharge(amount: String): ApiResponse<Wallet> {
        val body = buildJsonObject {
            put("amount", JsonPrimitive(amount))
        }.toString()
        val response = client.post("/wallet/recharge", body)
        return json.decodeFromString(ApiResponse.serializer(Wallet.serializer()), response)
    }

    suspend fun getTransactions(): ApiResponse<List<WalletTransaction>> {
        val response = client.get("/wallet/transactions")
        return json.decodeFromString(ApiResponse.serializer(kotlinx.serialization.builtins.ListSerializer(WalletTransaction.serializer())), response)
    }
}
