package org.xxg.backend.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新订单状态请求DTO
 * <p>管理员更新订单状态时提交的请求体</p>
 */
@Data
public class UpdateOrderStatusRequest {
    /** 订单号 */
    @NotBlank(message = "订单号不能为空")
    @Size(max = 64, message = "订单号长度不能超过64个字符")
    private String orderNo;

    /** 目标状态 */
    @NotBlank(message = "状态值不能为空")
    @Pattern(regexp = "^(completed|failed)$", message = "无效的状态值，仅支持 completed 或 failed")
    private String status;
}
