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

    /**
     * 时长卡的有效时长（天）。
     * <p>与 {@link #days} 语义相同，两者都表示天数（对应 {@code Card.duration} 字段）。
     * 若仅提供 duration 而未提供 days，服务端会自动以 duration 作为有效期，
     * 避免因客户端漏传 days 而生成永不过期的卡密。</p>
     */
    private Integer duration;

    /** 次数卡的总次数 */
    @Min(value = 1, message = "次数必须大于0")
    private Integer totalCount;

    /** 创建者类型：admin / user / system，默认 admin */
    @NotBlank(message = "创建者类型不能为空")
    private String creatorType = "admin";

    /** 创建者ID */
    private Integer creatorId = 1;
    /** 创建者名称 */
    private String creatorName = "admin";
    /** 验证方式：web / post / get */
    private String verifyMethod;
    /** 时长卡有效期天数（与 duration 等价，优先使用） */
    private Integer days;
    /** 关联的API密钥ID */
    private Integer apiKeyId;

    // 以下三个开关此前不在 DTO 中：前端生成弹窗提交后 Jackson 静默丢弃，
    // 新卡一律取实体默认值（allowReverify=true/stackTime=false/allowSelfUnbind=false），
    // 导致"允许重复验证/同机续期/允许自助解绑"三个开关全部无效
    /** 是否允许重复验证（null 取实体默认 true） */
    private Boolean allowReverify;
    /** 同机器码重复使用时是否叠加时长（null 取实体默认 false） */
    private Boolean stackTimeIfSameMachine;
    /** 是否允许用户自行解绑设备（null 取实体默认 false） */
    private Boolean allowSelfUnbind;

    /** 批量生成数量 */
    @Min(value = 1, message = "生成数量必须大于0")
    private Integer count = 1;

    /**
     * 获取时长卡有效天数，兼容只传 duration 的旧客户端。
     *
     * @return 有效天数，未提供时返回 null
     */
    public Integer resolveDays() {
        return days != null ? days : duration;
    }
}
