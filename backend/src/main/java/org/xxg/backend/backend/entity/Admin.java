package org.xxg.backend.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 管理员实体 - 系统后台管理员账号信息
 */
@Data
@Entity
@Table(name = "admins")
public class Admin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String username; // 管理员登录用户名

    @Column(nullable = false, length = 255)
    private String password; // 加密后的登录密码

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();

    @Column(name = "last_login")
    private LocalDateTime lastLogin; // 最后登录时间

    @Column(name = "access_token", length = 512)
    private String accessToken; // JWT访问令牌

    @Column(name = "refresh_token", length = 512)
    private String refreshToken; // JWT刷新令牌

    @Column(name = "totp_secret", length = 255)
    private String totpSecret; // TOTP双因素认证密钥

    @Column(name = "totp_enabled")
    private Boolean totpEnabled = false; // 是否启用TOTP双因素认证

    @Column(length = 100)
    private String email; // 管理员邮箱
}
