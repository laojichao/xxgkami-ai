package org.xxg.backend.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员更新用户资料请求DTO
 * <p>管理员更新用户资料时提交的请求体，所有字段可选。</p>
 */
@Data
public class UpdateUserRequest {
    /** 昵称 */
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;

    /** 邮箱 */
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;

    /** 手机号 */
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
