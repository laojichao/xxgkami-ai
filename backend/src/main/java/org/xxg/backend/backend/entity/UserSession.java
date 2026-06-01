package org.xxg.backend.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户会话实体 - 管理用户的登录会话信息
 */
@Data
@Entity
@Table(name = "user_sessions")
public class UserSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId; // 关联的用户ID

    @Column(name = "session_token", nullable = false, unique = true, length = 64)
    private String sessionToken; // 会话令牌(唯一标识)

    @Column(name = "device_info", length = 255)
    private String deviceInfo; // 设备信息(如: iPhone 15, Chrome等)

    @Column(name = "ip_address", length = 50)
    private String ipAddress; // 登录IP地址

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent; // 客户端浏览器/设备完整标识

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt; // 会话过期时间

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();

    @Column(name = "last_activity")
    private LocalDateTime lastActivity = LocalDateTime.now(); // 最后活跃时间(用于超时判断)
}
