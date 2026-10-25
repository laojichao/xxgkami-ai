package org.xxg.backend.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员更新用户资料请求DTO
 * <p>管理员更新用户资料时提交的请求体，所有字段可选。
 * status 为 null 时不改动账号状态（与 nickname/email/phone 的"可选"语义一致）。</p>
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

    /**
     * 账号启用状态（true=启用，false=禁用），null 表示不修改。
     * <p>前端用户管理弹窗提供状态下拉框，此前该字段被 Jackson 静默忽略，
     * 管理员在编辑弹窗中修改状态完全无效。</p>
     */
    private Boolean status;
}
