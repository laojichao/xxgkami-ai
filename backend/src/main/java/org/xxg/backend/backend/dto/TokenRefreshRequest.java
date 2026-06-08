package org.xxg.backend.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Token刷新请求DTO
 * <p>使用刷新令牌获取新的访问令牌</p>
 */
@Data
public class TokenRefreshRequest {
    /** 刷新令牌 */
    @NotBlank(message = "refreshToken不能为空")
    private String refreshToken;
}
