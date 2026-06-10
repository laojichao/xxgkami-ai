package org.xxg.backend.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.Objects;
import java.time.LocalDateTime;

/**
 * 卡密实体 - 系统核心业务对象，存储所有卡密的完整信息
 */
@Getter
@Setter
@Entity
@Table(name = "cards", indexes = {
    @Index(name = "idx_card_creator", columnList = "creator_type, creator_id"),
    @Index(name = "idx_card_machine_code", columnList = "machine_code")
})
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "card_key", length = 512, unique = true)
    private String cardKey; // 卡密明文(原始卡密值)

    @Column(name = "encrypted_key", unique = true, length = 255)
    private String encryptedKey; // 加密后的卡密(用于安全存储)

    private Integer status = 0; // 卡密状态: 0=未使用, 1=已使用, 2=已停用

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();

    @Column(name = "use_time")
    private LocalDateTime useTime; // 卡密首次使用时间

    @Column(name = "expire_time")
    private LocalDateTime expireTime; // 卡密过期时间

    private Integer duration = 0; // 有效时长(天数, 时长卡使用)

    @Enumerated(EnumType.STRING)
    @Column(name = "verify_method", columnDefinition = "enum('web','post','get')")
    private VerifyMethod verifyMethod; // 验证方式: web=网页, post=POST接口, get=GET接口

    @Column(name = "allow_reverify")
    private Boolean allowReverify = true; // 是否允许重复验证

    @Column(name = "device_id", length = 64)
    private String deviceId; // 绑定的设备ID

    @Column(name = "encryption_type", length = 50)
    private String encryptionType; // 加密类型(如AES, RSA等)

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", columnDefinition = "enum('time','count')")
    private CardType cardType = CardType.time; // 卡密类型: time=时长卡, count=次数卡

    @Column(name = "total_count")
    private Integer totalCount = 0; // 总次数(次数卡使用)

    @Column(name = "remaining_count")
    private Integer remainingCount = 0; // 剩余次数(次数卡使用)

    @Enumerated(EnumType.STRING)
    @Column(name = "creator_type", columnDefinition = "enum('admin','user','system')")
    private CreatorType creatorType = CreatorType.admin; // 创建者类型: admin=管理员, user=普通用户

    @Column(name = "creator_id")
    private Integer creatorId; // 创建者ID

    @Column(name = "creator_name", length = 50)
    private String creatorName; // 创建者名称

    @Column(name = "ip_address", length = 255)
    private String ipAddress; // 使用卡密时的IP地址

    @Column(name = "machine_code", length = 255)
    private String machineCode; // 绑定的机器码

    @Column(name = "api_key_id")
    private Integer apiKeyId; // 通过API创建时关联的API密钥ID

    @Column(name = "stack_time_if_same_machine")
    private Boolean stackTimeIfSameMachine = false; // 同机器码重复使用时是否叠加时长

    @Column(name = "merged_into_card_id")
    private Integer mergedIntoCardId; // 合并目标卡密ID(本卡密被合并到的目标)

    @Column(name = "allow_self_unbind")
    private Boolean allowSelfUnbind = false; // 是否允许用户自行解绑设备

    /** 验证方式枚举 */
    public enum VerifyMethod { web, post, get }
    /** 卡密类型枚举: time=时长卡, count=次数卡 */
    public enum CardType { time, count }
    /** 创建者类型枚举: admin=管理员, user=普通用户, system=系统自动生成 */
    public enum CreatorType { admin, user, system }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Card that = (Card) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
