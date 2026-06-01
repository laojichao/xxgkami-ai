package org.xxg.backend.backend.dto;

import lombok.Data;

/**
 * 注册绑定请求DTO
 * <p>第三方登录或邮箱验证后，将临时token与用户账号绑定</p>
 */
@Data
public class RegisterBindRequest {
    private Integer userId;
    /** 临时验证token */
    private String token;
}
