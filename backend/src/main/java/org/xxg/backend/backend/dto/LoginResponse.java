package org.xxg.backend.backend.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.Map;

/**
 * 登录响应DTO
 * <p>登录成功后返回的令牌和用户信息</p>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    /** 访问令牌，用于后续请求认证 */
    private String token;
    /** 刷新令牌，用于无感刷新访问令牌 */
    private String refreshToken;
    /** 用户基本信息 */
    private Map<String, Object> userInfo;
}
