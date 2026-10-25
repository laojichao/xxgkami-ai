package org.xxg.backend.backend.dto;

import lombok.Getter;
import lombok.Setter;
import org.xxg.backend.backend.entity.Card;

import java.time.LocalDateTime;

/**
 * 面向普通用户的卡密响应 DTO（脱敏）。
 *
 * <p><b>设计动机：</b>原 {@code /cards/user/me} 直接复用 {@link CardResponse}，
 * 而 CardResponse 的类注释明确写着"普通用户接口不得使用此 DTO"——它返回
 * encryptedKey（卡密哈希）、deviceId、creatorId 等内部字段。用户端只需要
 * 展示自己卡密的状态与有效期，无需任何内部标识。</p>
 *
 * <p>字段白名单：cardKey（明文，用户本来就知道自己的卡密）、status、cardType、
 * totalCount/remainingCount、expireTime/createTime/useTime（展示用）、
 * machineCode（用户需知道自己绑定了什么设备才能自助解绑）。</p>
 */
@Getter
@Setter
public class UserCardResponse {
    private Integer id;
    /** 卡密明文（用户自己的卡密，用于展示/复制/核销） */
    private String cardKey;
    /** 0=未使用 1=已使用 2=已停用 */
    private Integer status;
    /** time / count */
    private String cardType;
    private Integer totalCount;
    private Integer remainingCount;
    private LocalDateTime createTime;
    private LocalDateTime useTime;
    private LocalDateTime expireTime;
    /** 绑定的机器码（自助解绑时需要用户确认原值） */
    private String machineCode;
    /** 是否允许自助解绑（前端据此决定是否显示解绑入口） */
    private Boolean allowSelfUnbind;
    /** 是否允许重复验证 */
    private Boolean allowReverify;
    /** 最近一次使用的来源 IP（用户查看自己卡密的使用记录） */
    private String ipAddress;

    /**
     * 从 Card 实体构建用户端脱敏响应。
     * <p>刻意不包含：encryptedKey（卡密哈希）、deviceId（历史空字段）、
     * creatorType/creatorId/creatorName、apiKeyId、verifyMethod、
     * stackTimeIfSameMachine 等内部字段。</p>
     */
    public static UserCardResponse fromEntity(Card card) {
        UserCardResponse resp = new UserCardResponse();
        resp.setId(card.getId());
        resp.setCardKey(card.getCardKey());
        resp.setStatus(card.getStatus());
        resp.setCardType(card.getCardType() != null ? card.getCardType().name() : null);
        resp.setTotalCount(card.getTotalCount());
        resp.setRemainingCount(card.getRemainingCount());
        resp.setCreateTime(card.getCreateTime());
        resp.setUseTime(card.getUseTime());
        resp.setExpireTime(card.getExpireTime());
        resp.setMachineCode(card.getMachineCode());
        resp.setAllowSelfUnbind(card.getAllowSelfUnbind());
        resp.setAllowReverify(card.getAllowReverify());
        resp.setIpAddress(card.getIpAddress());
        return resp;
    }
}
