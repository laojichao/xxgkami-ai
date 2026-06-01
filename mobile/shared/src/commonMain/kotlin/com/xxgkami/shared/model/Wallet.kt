package com.xxgkami.shared.model

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
    val userId: Int? = null,
    val balance: String? = null,
    val totalRecharge: String? = null,
    val totalConsume: String? = null
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
    val balanceAfter: String? = null,
    val description: String? = null,
    val createTime: String? = null
)
