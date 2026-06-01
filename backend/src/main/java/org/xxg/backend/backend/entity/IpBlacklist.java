package org.xxg.backend.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * IP黑名单实体 - 记录被封禁的IP地址及其封禁原因
 */
@Data
@Entity
@Table(name = "ip_blacklist")
public class IpBlacklist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String ipAddress; // 被封禁的IP地址

    @Column(length = 255)
    private String reason; // 封禁原因

    @Column(name = "blocked_until")
    private LocalDateTime blockedUntil; // 封禁截止时间(临时封禁使用)

    private Boolean permanent = false; // 是否永久封禁

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();
}
