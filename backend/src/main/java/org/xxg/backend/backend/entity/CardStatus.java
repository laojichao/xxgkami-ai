package org.xxg.backend.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "card_status")
public class CardStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_hash", nullable = false, unique = true, length = 255)
    private String cardHash;

    @Column(name = "remain_count")
    private Integer remainCount = 0;

    @Column(name = "total_count")
    private Integer totalCount = 0;

    @Column(name = "expire_time")
    private LocalDateTime expireTime;

    @Column(name = "last_use_time")
    private LocalDateTime lastUseTime;

    @Column(name = "is_valid")
    private Boolean isValid = true;
}
