package org.xxg.backend.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "access_logs")
public class AccessLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String ip;

    @Column(length = 10)
    private String method;

    @Column(length = 500)
    private String uri;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    private Integer status;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(length = 50)
    private String username;

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();
}
