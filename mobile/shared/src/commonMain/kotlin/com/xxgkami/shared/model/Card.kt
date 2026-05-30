package com.xxgkami.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class Card(
    val id: Int? = null,
    val cardKey: String? = null,
    val status: Int? = null,
    val cardType: String? = null,
    val totalCount: Int? = null,
    val remainingCount: Int? = null,
    val expireTime: String? = null,
    val machineCode: String? = null,
    val createTime: String? = null
)

@Serializable
data class CardVerifyRequest(val cardKey: String, val machineCode: String? = null)

@Serializable
data class CardVerifyResponse(
    val success: Boolean,
    val message: String? = null,
    val statusCode: Int? = null,
    val remainingCount: Int? = null,
    val remainingTime: Long? = null,
    val expireTime: String? = null
)
