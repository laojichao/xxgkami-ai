package org.xxg.backend.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员创建用户请求DTO
 * <p>管理员通过后台创建新用户时提交的请求体，包含完整的密码强度校验。</p>
 */
@Data
public class CreateUserRequest {
    /** 用户名 */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    private String username;

    /** 密码（明文，将被 BCrypt 加密存储） */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 50, message = "密码长度必须在8-50位之间")
    @Pattern(regexp = ".*[A-Z].*", message = "密码必须包含至少一个大写字母")
    @Pattern(regexp = ".*[a-z].*", message = "密码必须包含至少一个小写字母")
    @Pattern(regexp = ".*\\d.*", message = "密码必须包含至少一个数字")
    private String password;

    /** 邮箱 */
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;

    /** 昵称 */
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;
}
