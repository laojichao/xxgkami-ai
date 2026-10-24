package org.xxg.backend.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册绑定请求DTO（第三方 OAuth 登录后完善账号信息）
 * <p>携带 OAuth 网关回调下发的 registerToken；邮箱为可选，无需邮箱验证码。</p>
 */
@Data
public class RegisterBindRequest {
    /** 用户名：字母/数字/下划线/连字符/中文，3-50 位（具体格式在服务端校验） */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在 3-50 个字符之间")
    private String username;
    /** 密码 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 50, message = "密码长度必须在 8-50 个字符之间")
    private String password;
    /** 邮箱（可选） */
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;
    /** OAuth 网关下发的一次性注册令牌 */
    @NotBlank(message = "注册令牌不能为空")
    private String registerToken;
}
