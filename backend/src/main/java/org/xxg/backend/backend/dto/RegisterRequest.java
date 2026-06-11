package org.xxg.backend.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求DTO
 * <p>新用户注册时提交的账号信息，需配合邮箱验证码使用</p>
 */
@Data
public class RegisterRequest {
    /** 用户名，长度3-50位 */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50之间")
    private String username;
    /** 密码，长度8-50位 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 50, message = "密码长度必须在8-50之间")
    private String password;
    /** 注册邮箱 */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    /** 邮箱验证码 */
    @NotBlank(message = "验证码不能为空")
    private String code;
}
