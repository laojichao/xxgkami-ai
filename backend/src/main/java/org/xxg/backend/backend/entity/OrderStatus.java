package org.xxg.backend.backend.entity;

/**
 * 订单状态枚举
 * <p>定义订单生命周期中的所有状态，用于规范状态值，防止字符串拼写错误。</p>
 * <p>注意：Order 实体的 status 字段保持 String 类型以兼容数据库现有数据，
 * 业务代码应使用此枚举的 name() 方法进行状态比较。</p>
 */
public enum OrderStatus {
    /** 待支付 */
    pending,
    /** 已完成（已支付并生成卡密） */
    completed,
    /** 已失败 */
    failed,
    /** 已取消（超时未支付自动取消） */
    cancelled
}
