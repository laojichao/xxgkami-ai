package org.xxg.backend.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * TOTP 验证码请求DTO
 * <p>用于 TOTP 启用/禁用时提交验证码</p>
 */
@Data
public class TotpCodeRequest {
    /** TOTP 验证码（6位数字） */
    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码必须为6位数字")
    private String code;
}
