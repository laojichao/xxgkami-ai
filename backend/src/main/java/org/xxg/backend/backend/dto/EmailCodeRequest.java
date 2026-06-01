package org.xxg.backend.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 邮箱验证码请求DTO
 * <p>请求发送邮箱验证码，支持注册、重置密码等场景</p>
 */
@Data
public class EmailCodeRequest {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    /** 验证码用途类型，如 register/resetPassword，默认 register */
    private String type = "register";
}
