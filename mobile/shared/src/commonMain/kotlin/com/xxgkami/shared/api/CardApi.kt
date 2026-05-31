package com.xxgkami.shared.api

import com.xxgkami.shared.model.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

class CardApi(private val client: ApiClient) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun useCard(cardKey: String, deviceId: String = "Unknown", ipAddress: String = ""): CardVerifyResponse {
        val body = buildJsonObject {
            put("card_key", JsonPrimitive(cardKey))
            put("device_id", JsonPrimitive(deviceId))
            put("ip_address", JsonPrimitive(ipAddress))
        }.toString()
        val response = client.post("/cards/use", body)
        return json.decodeFromString(CardVerifyResponse.serializer(), response)
    }

    suspend fun getUserCards(userId: Int): ApiResponse<List<Card>> {
        val response = client.get("/cards/user/$userId")
        return json.decodeFromString(ApiResponse.serializer(kotlinx.serialization.builtins.ListSerializer(Card.serializer())), response)
    }

    suspend fun machineBindQuery(cardKey: String): ApiResponse<Map<String, String>> {
        val body = buildJsonObject {
            put("card_key", JsonPrimitive(cardKey))
        }.toString()
        val response = client.post("/public/cards/machine-bind/query", body)
        return json.decodeFromString(ApiResponse.serializer(kotlinx.serialization.builtins.MapSerializer(kotlinx.serialization.builtins.serializer<String>(), kotlinx.serialization.builtins.serializer<String>())), response)
    }

    suspend fun machineUnbind(cardKey: String, machineCode: String): ApiResponse<Unit> {
        val body = buildJsonObject {
            put("card_key", JsonPrimitive(cardKey))
            put("machine_code", JsonPrimitive(machineCode))
        }.toString()
        val response = client.post("/public/cards/machine-bind/unbind", body)
        return json.decodeFromString(ApiResponse.serializer(kotlinx.serialization.builtins.serializer<Unit>()), response)
    }
}
