package org.xxg.backend.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GenerateCardRequest {
    @NotBlank(message = "卡密类型不能为空")
    private String cardType = "time";

    private Integer duration;

    @Min(value = 1, message = "次数必须大于0")
    private Integer totalCount;

    @NotBlank(message = "创建者类型不能为空")
    private String creatorType = "admin";

    private Integer creatorId = 1;
    private String creatorName = "admin";
    private String verifyMethod;
    private Integer days;
    private Long apiKeyId;
}
