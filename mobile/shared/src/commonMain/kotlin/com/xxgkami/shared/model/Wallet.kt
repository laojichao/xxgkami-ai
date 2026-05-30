package com.xxgkami.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class Wallet(
    val id: Long? = null,
    val userId: Int? = null,
    val balance: String? = null,
    val totalRecharge: String? = null,
    val totalConsume: String? = null
)

@Serializable
data class WalletTransaction(
    val id: Long? = null,
    val type: String? = null,
    val amount: String? = null,
    val balanceAfter: String? = null,
    val description: String? = null,
    val createTime: String? = null
)
