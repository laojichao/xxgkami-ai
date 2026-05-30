package com.xxgkami.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class Order(
    val id: Int? = null,
    val orderNo: String? = null,
    val cardType: String? = null,
    val cardSpec: String? = null,
    val quantity: Int? = null,
    val totalPrice: String? = null,
    val status: String? = null,
    val paymentMethod: String? = null,
    val createTime: String? = null
)
