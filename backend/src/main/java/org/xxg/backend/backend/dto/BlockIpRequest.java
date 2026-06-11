package org.xxg.backend.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 封禁IP请求DTO
 * <p>管理员封禁指定IP时提交的请求体</p>
 */
@Data
public class BlockIpRequest {
    /** IP地址 */
    @NotBlank(message = "IP地址不能为空")
    @Pattern(regexp = "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$|^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$",
            message = "IP地址格式无效")
    private String ip;

    /** 封禁原因 */
    @Size(max = 255, message = "原因长度不能超过255个字符")
    private String reason;

    /** 封禁时长（小时），为空则永久封禁 */
    @Min(value = 1, message = "封禁时长至少为1小时")
    @Max(value = 8760, message = "封禁时长不能超过8760小时（1年）")
    private Integer hours;
}
