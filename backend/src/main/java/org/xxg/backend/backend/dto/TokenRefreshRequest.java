package org.xxg.backend.backend.dto;

import lombok.Data;

@Data
public class TokenRefreshRequest {
    private String refreshToken;
}
