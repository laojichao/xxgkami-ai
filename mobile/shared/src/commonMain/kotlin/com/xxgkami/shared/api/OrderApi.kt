package com.xxgkami.shared.api

import com.xxgkami.shared.model.*
import kotlinx.serialization.json.Json

class OrderApi(private val client: ApiClient) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getMyOrders(): ApiResponse<List<Order>> {
        val response = client.get("/orders")
        return json.decodeFromString(ApiResponse.serializer(kotlinx.serialization.builtins.ListSerializer(Order.serializer())), response)
    }

    suspend fun createOrder(data: String): ApiResponse<Order> {
        val response = client.post("/orders", data)
        return json.decodeFromString(ApiResponse.serializer(Order.serializer()), response)
    }
}
