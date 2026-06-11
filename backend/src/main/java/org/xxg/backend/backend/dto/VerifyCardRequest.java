package org.xxg.backend.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 验证卡密请求DTO
 * <p>客户端提交卡密进行激活验证</p>
 */
@Data
public class VerifyCardRequest {
    /** 卡密字符串 */
    @NotBlank(message = "卡密不能为空")
    private String cardKey;
    /** 机器码，用于绑定设备 */
    @Size(max = 255, message = "机器码长度不能超过255个字符")
    private String machineCode;
    /** API密钥ID */
    private Integer apiKeyId;
}
