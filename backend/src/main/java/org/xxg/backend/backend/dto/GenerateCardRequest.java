package org.xxg.backend.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 生成卡密请求DTO
 * <p>管理员或API批量生成卡密时提交的参数</p>
 */
@Data
public class GenerateCardRequest {
    /** 卡密类型：time(时长卡) / count(次数卡)，默认 time */
    @NotBlank(message = "卡密类型不能为空")
    private String cardType = "time";

    /** 时长卡的有效时长（分钟） */
    private Integer duration;

    /** 次数卡的总次数 */
    @Min(value = 1, message = "次数必须大于0")
    private Integer totalCount;

    /** 创建者类型：admin / api，默认 admin */
    @NotBlank(message = "创建者类型不能为空")
    private String creatorType = "admin";

    /** 创建者ID */
    private Integer creatorId = 1;
    /** 创建者名称 */
    private String creatorName = "admin";
    /** 验证方式 */
    private String verifyMethod;
    /** 有效期天数 */
    private Integer days;
    /** 关联的API密钥ID */
    private Integer apiKeyId;

    /** 批量生成数量（前端传 count，后端循环生成） */
    @Min(value = 1, message = "生成数量必须大于0")
    private Integer count = 1;
}
