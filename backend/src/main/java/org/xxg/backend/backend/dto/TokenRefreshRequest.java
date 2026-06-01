package org.xxg.backend.backend.dto;

import lombok.Data;

/**
 * Token刷新请求DTO
 * <p>使用刷新令牌获取新的访问令牌</p>
 */
@Data
public class TokenRefreshRequest {
    /** 刷新令牌 */
    private String refreshToken;
}
