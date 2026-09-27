package org.xxg.backend.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建订单请求DTO
 * <p>用户购买卡密时提交的订单信息</p>
 */
@Data
public class CreateOrderRequest {
    private Integer userId;
    private String username;
    /** 卡密类型，如 time/count 等 */
    @NotBlank(message = "卡密类型不能为空")
    private String cardType;
    /** 卡密规格 */
    private String cardSpec;
    /** 购买数量，默认1 */
    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量至少为1")
    private Integer quantity = 1;
    /** 支付方式 */
    @NotBlank(message = "支付方式不能为空")
    private String paymentMethod;
    /**
     * 接收邮箱（已废弃，服务端忽略此字段）。
     * <p>卡密投递地址一律取自用户账号绑定的邮箱（users.email），
     * 不采信客户端传入值，避免卡密被投递到攻击者指定的地址。
     * 保留该字段仅为兼容旧版客户端请求体，请勿用于业务逻辑。</p>
     *
     * @deprecated 使用用户账号邮箱，请勿依赖此字段
     */
    @Deprecated
    private String email;
    /** 关联的定价策略ID */
    @NotNull(message = "定价策略ID不能为空")
    private Integer pricingId;
}
