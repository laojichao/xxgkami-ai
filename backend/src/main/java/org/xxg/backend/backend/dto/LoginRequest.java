package org.xxg.backend.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 登录请求DTO
 * <p>用户使用用户名和密码登录时提交的请求体</p>
 */
@Data
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名长度不能超过50个字符")
    private String username;
    @NotBlank(message = "密码不能为空")
    @Size(max = 128, message = "密码长度不能超过128个字符")
    private String password;
    /** TOTP 二次验证码（管理员启用 TOTP 时必填） */
    private String totpCode;
}
