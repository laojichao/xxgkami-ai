package org.xxg.backend.backend.dto;

import lombok.Getter;
import lombok.Setter;
import org.xxg.backend.backend.entity.Card;

import java.time.LocalDateTime;

/**
 * 卡密响应 DTO。
 * <p>用于向前端返回卡密信息，包含卡密明文。
 * 不直接使用 Card 实体序列化，避免 @JsonIgnore 导致 cardKey 缺失。</p>
 * <p><b>安全边界：</b>所有使用此 DTO 的端点（/cards/admin/**、/cards/user/**、
 * /cards/apikey/**）在 SecurityConfig 中均要求 ADMIN 角色，因此可以返回卡密明文。
 * 管理员必须能够复制/导出卡密以交付给客户，脱敏会导致核心业务流程不可用。
 * 面向普通用户的接口不得使用此 DTO。</p>
 */
@Getter
@Setter
public class CardResponse {
    private Integer id;
    private String cardKey; // 卡密明文（仅限管理员接口返回）
    private String encryptedKey;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime useTime;
    private LocalDateTime expireTime;
    private Integer duration;
    private String verifyMethod;
    private Boolean allowReverify;
    private String deviceId;
    private String cardType;
    private Integer totalCount;
    private Integer remainingCount;
    private String creatorType;
    private Integer creatorId;
    private String creatorName;
    private String machineCode;
    private Integer apiKeyId;
    private Boolean stackTimeIfSameMachine;
    private Boolean allowSelfUnbind;

    /**
     * 从 Card 实体构建 CardResponse。
     * <p>返回完整卡密明文——调用方必须是管理员接口（见类注释的安全边界说明）。</p>
     */
    public static CardResponse fromEntity(Card card) {
        CardResponse resp = new CardResponse();
        resp.setId(card.getId());
        // 返回完整卡密明文：管理员需要复制/导出卡密交付客户，脱敏会使业务不可用
        resp.setCardKey(card.getCardKey());
        resp.setEncryptedKey(card.getEncryptedKey());
        resp.setStatus(card.getStatus());
        resp.setCreateTime(card.getCreateTime());
        resp.setUseTime(card.getUseTime());
        resp.setExpireTime(card.getExpireTime());
        resp.setDuration(card.getDuration());
        resp.setVerifyMethod(card.getVerifyMethod() != null ? card.getVerifyMethod().name() : null);
        resp.setAllowReverify(card.getAllowReverify());
        resp.setDeviceId(card.getDeviceId());
        resp.setCardType(card.getCardType() != null ? card.getCardType().name() : null);
        resp.setTotalCount(card.getTotalCount());
        resp.setRemainingCount(card.getRemainingCount());
        resp.setCreatorType(card.getCreatorType() != null ? card.getCreatorType().name() : null);
        resp.setCreatorId(card.getCreatorId());
        resp.setCreatorName(card.getCreatorName());
        resp.setMachineCode(card.getMachineCode());
        resp.setApiKeyId(card.getApiKeyId());
        resp.setStackTimeIfSameMachine(card.getStackTimeIfSameMachine());
        resp.setAllowSelfUnbind(card.getAllowSelfUnbind());
        return resp;
    }
}
