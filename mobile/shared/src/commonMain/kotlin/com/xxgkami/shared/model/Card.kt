package com.xxgkami.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 卡密数据模型
 * 表示一张卡密的完整信息
 *
 * @property id 卡密ID
 * @property cardKey 卡密字符串
 * @property status 卡密状态（如：未使用、已使用、已过期）
 * @property cardType 卡密类型（如：次数卡、时长卡）
 * @property totalCount 总使用次数
 * @property remainingCount 剩余使用次数
 * @property expireTime 过期时间
 * @property machineCode 绑定的机器码
 * @property createTime 创建时间
 */
@Serializable
data class Card(
    val id: Int? = null,
    @SerialName("card_key") val cardKey: String? = null,
    val status: Int? = null,
    @SerialName("card_type") val cardType: String? = null,
    @SerialName("total_count") val totalCount: Int? = null,
    @SerialName("remaining_count") val remainingCount: Int? = null,
    @SerialName("expire_time") val expireTime: String? = null,
    @SerialName("machine_code") val machineCode: String? = null,
    @SerialName("create_time") val createTime: String? = null
)

/** 卡密验证请求 */
@Serializable
data class CardVerifyRequest(@SerialName("card_key") val cardKey: String, @SerialName("machine_code") val machineCode: String? = null)

/**
 * 卡密验证响应
 *
 * @property success 验证是否成功
 * @property message 响应消息
 * @property statusCode 状态码
 * @property remainingCount 剩余次数
 * @property remainingTime 剩余时间（秒）
 * @property expireTime 到期时间
 */
@Serializable
data class CardVerifyResponse(
    val success: Boolean,
    val message: String? = null,
    @SerialName("status_code") val statusCode: Int? = null,
    @SerialName("remaining_count") val remainingCount: Int? = null,
    @SerialName("remaining_time") val remainingTime: Long? = null,
    @SerialName("expire_time") val expireTime: String? = null
)
