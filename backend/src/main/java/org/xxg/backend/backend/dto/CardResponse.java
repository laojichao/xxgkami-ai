package org.xxg.backend.backend.dto;

import lombok.Getter;
import lombok.Setter;
import org.xxg.backend.backend.entity.Card;

import java.time.LocalDateTime;

/**
 * 卡密响应 DTO。
 * <p>用于向前端返回卡密信息，包含脱敏后的卡密明文。
 * 不直接使用 Card 实体序列化，避免 @JsonIgnore 导致 cardKey 缺失。</p>
 */
@Getter
@Setter
public class CardResponse {
    private Integer id;
    private String cardKey; // 脱敏后的卡密（前4位****后4位）
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
     * 从 Card 实体构建 CardResponse，对 cardKey 进行脱敏处理
     */
    public static CardResponse fromEntity(Card card) {
        CardResponse resp = new CardResponse();
        resp.setId(card.getId());
        // 卡密脱敏：前4位****后4位
        String key = card.getCardKey();
        if (key != null && key.length() > 8) {
            resp.setCardKey(key.substring(0, 4) + "****" + key.substring(key.length() - 4));
        } else if (key != null) {
            resp.setCardKey("****");
        } else {
            resp.setCardKey(null);
        }
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
