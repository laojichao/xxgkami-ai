package org.xxg.backend.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 重置密码请求DTO
 * <p>用户通过邮箱验证码重置密码时提交的请求体</p>
 */
@Data
public class ResetPasswordRequest {
    /** 绑定的邮箱 */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    /** 邮箱验证码 */
    @NotBlank(message = "验证码不能为空")
    private String code;
    /** 新密码，长度6-50位 */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度必须在6-50之间")
    private String newPassword;
}
