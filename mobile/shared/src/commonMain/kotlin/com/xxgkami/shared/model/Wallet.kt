package com.xxgkami.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 钱包数据模型
 * 表示用户的数字钱包信息
 *
 * @property id 钱包ID
 * @property userId 所属用户ID
 * @property balance 当前余额
 * @property totalRecharge 累计充值金额
 * @property totalConsume 累计消费金额
 */
@Serializable
data class Wallet(
    val id: Long? = null,
    @SerialName("user_id") val userId: Int? = null,
    val balance: String? = null,
    @SerialName("total_recharge") val totalRecharge: String? = null,
    @SerialName("total_consume") val totalConsume: String? = null
)

/**
 * 钱包交易记录模型
 *
 * @property id 交易ID
 * @property type 交易类型（recharge: 充值, consume: 消费）
 * @property amount 交易金额
 * @property balanceAfter 交易后余额
 * @property description 交易描述
 * @property createTime 交易时间
 */
@Serializable
data class WalletTransaction(
    val id: Long? = null,
    val type: String? = null,
    val amount: String? = null,
    @SerialName("balance_after") val balanceAfter: String? = null,
    val description: String? = null,
    @SerialName("create_time") val createTime: String? = null
)
