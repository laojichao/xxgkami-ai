package org.xxg.backend.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyCardRequest {
    @NotBlank(message = "卡密不能为空")
    private String cardKey;
    private String machineCode;
    private Long apiKeyId;
}
