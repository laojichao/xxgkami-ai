package org.xxg.backend.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建API Key请求DTO
 * <p>管理员创建新API Key时提交的请求体</p>
 */
@Data
public class ApiKeyCreateRequest {
    /** 密钥名称 */
    @NotBlank(message = "密钥名称不能为空")
    @Size(max = 100, message = "名称长度不能超过100个字符")
    private String name;

    /** 密钥描述 */
    @Size(max = 500, message = "描述长度不能超过500个字符")
    private String description;

    /** 是否启用卡密加密传输 */
    private Boolean enableCardEncryption;

    /** 是否要求绑定机器码 */
    private Boolean requireMachineCode;

    /** Webhook回调配置(JSON格式) */
    private String webhookConfig;

    /** 机器码一次性绑定配置(JSON格式) */
    private String machineSpecOnceConfig;
}
