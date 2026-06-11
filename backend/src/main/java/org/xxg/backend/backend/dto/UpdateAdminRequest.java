package org.xxg.backend.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新管理员信息请求DTO
 * <p>管理员更新个人信息时提交的请求体</p>
 */
@Data
public class UpdateAdminRequest {
    /** 新邮箱地址 */
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    @Email(message = "邮箱格式不正确")
    private String email;
}
