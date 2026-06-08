package org.xxg.backend.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 注册绑定请求DTO
 * <p>第三方登录或邮箱验证后，将临时token与用户账号绑定</p>
 */
@Data
public class RegisterBindRequest {
    @NotNull(message = "用户ID不能为空")
    private Integer userId;
    /** 临时验证token */
    @NotBlank(message = "绑定token不能为空")
    private String token;
}
