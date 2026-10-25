package com.xxgkami.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 订单数据模型
 * 表示一个购买卡密的订单
 *
 * @property id 订单ID
 * @property orderNo 订单编号
 * @property cardType 卡密类型
 * @property cardSpec 卡密规格
 * @property quantity 购买数量
 * @property unitPrice 单价
 * @property totalPrice 总价
 * @property status 订单状态
 * @property paymentMethod 支付方式
 * @property createTime 创建时间
 * @property payTime 支付完成时间
 * @property cardKeys 购买的卡密内容（逗号分隔）。
 *   后端 Order.cardKeys 注释明言前端订单页依赖此字段；此前模型缺该字段，
 *   移动端订单页永远看不到/无法复制已购卡密
 */
@Serializable
data class Order(
    val id: Int? = null,
    val orderNo: String? = null,
    val cardType: String? = null,
    val cardSpec: String? = null,
    val quantity: Int? = null,
    val unitPrice: String? = null,
    val totalPrice: String? = null,
    val status: String? = null,
    val paymentMethod: String? = null,
    val createTime: String? = null,
    val payTime: String? = null,
    val cardKeys: String? = null
)
