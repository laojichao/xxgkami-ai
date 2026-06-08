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
    /** 接收邮箱 */
    private String email;
    /** 关联的定价策略ID */
    @NotNull(message = "定价策略ID不能为空")
    private Integer pricingId;
}
