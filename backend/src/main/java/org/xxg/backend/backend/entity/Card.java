package org.xxg.backend.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "cards")
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "card_key", length = 512, unique = true)
    private String cardKey;

    @Column(name = "encrypted_key", unique = true, length = 255)
    private String encryptedKey;

    private Integer status = 0; // 0:未使用 1:已使用 2:已停用

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();

    @Column(name = "use_time")
    private LocalDateTime useTime;

    @Column(name = "expire_time")
    private LocalDateTime expireTime;

    private Integer duration = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "verify_method", columnDefinition = "enum('web','post','get')")
    private VerifyMethod verifyMethod;

    @Column(name = "allow_reverify")
    private Boolean allowReverify = true;

    @Column(name = "device_id", length = 64)
    private String deviceId;

    @Column(name = "encryption_type", length = 50)
    private String encryptionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", columnDefinition = "enum('time','count')")
    private CardType cardType = CardType.time;

    @Column(name = "total_count")
    private Integer totalCount = 0;

    @Column(name = "remaining_count")
    private Integer remainingCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "creator_type", columnDefinition = "enum('admin','user')")
    private CreatorType creatorType = CreatorType.admin;

    @Column(name = "creator_id")
    private Integer creatorId;

    @Column(name = "creator_name", length = 50)
    private String creatorName;

    @Column(name = "ip_address", length = 255)
    private String ipAddress;

    @Column(name = "machine_code", length = 255)
    private String machineCode;

    @Column(name = "api_key_id")
    private Long apiKeyId;

    @Column(name = "stack_time_if_same_machine")
    private Boolean stackTimeIfSameMachine = false;

    @Column(name = "merged_into_card_id")
    private Long mergedIntoCardId;

    @Column(name = "allow_self_unbind")
    private Boolean allowSelfUnbind = false;

    public enum VerifyMethod { web, post, get }
    public enum CardType { time, count }
    public enum CreatorType { admin, user }
}
