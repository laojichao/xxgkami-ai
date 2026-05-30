package org.xxg.backend.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ip_blacklist")
public class IpBlacklist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String ipAddress;

    @Column(length = 255)
    private String reason;

    @Column(name = "blocked_until")
    private LocalDateTime blockedUntil;

    private Boolean permanent = false;

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();
}
