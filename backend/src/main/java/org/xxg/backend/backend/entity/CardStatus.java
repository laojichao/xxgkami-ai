package org.xxg.backend.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * 卡密状态实体 - 独立存储卡密的实时使用状态，用于快速查询验证
 */
@Data
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "card_status")
public class CardStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_hash", nullable = false, unique = true, length = 255)
    private String cardHash; // 卡密哈希值(与CardCipher关联)

    @Column(name = "remain_count")
    private Integer remainCount = 0; // 剩余使用次数

    @Column(name = "total_count")
    private Integer totalCount = 0; // 总可用次数

    @Column(name = "expire_time")
    private LocalDateTime expireTime; // 过期时间

    @Column(name = "last_use_time")
    private LocalDateTime lastUseTime; // 最后使用时间

    @Column(name = "is_valid")
    private Boolean isValid = true; // 是否有效
}
