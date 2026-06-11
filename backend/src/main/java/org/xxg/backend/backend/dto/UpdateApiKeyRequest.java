package org.xxg.backend.backend.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新API Key请求DTO
 * <p>管理员更新API Key信息时提交的请求体</p>
 */
@Data
public class UpdateApiKeyRequest {
    /** 密钥名称 */
    @Size(max = 100, message = "名称长度不能超过100个字符")
    private String name;

    /** 密钥描述 */
    @Size(max = 500, message = "描述长度不能超过500个字符")
    private String description;

    /** 密钥状态 */
    private Boolean status;
}
