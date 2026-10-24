package org.xxg.backend.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 绑定令牌验证请求DTO
 * <p>客户端绑定流程中验证临时 token 是否归属指定用户</p>
 */
@Data
public class BindValidateRequest {
    @NotNull(message = "用户ID不能为空")
    private Integer userId;
    /** 临时验证token */
    @NotBlank(message = "绑定token不能为空")
    private String token;
}
