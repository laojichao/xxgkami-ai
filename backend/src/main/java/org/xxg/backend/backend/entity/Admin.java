package org.xxg.backend.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.Objects;
import java.time.LocalDateTime;

/**
 * 管理员实体 - 系统后台管理员账号信息
 */
@Getter
@Setter
@Entity
@Table(name = "admins")
public class Admin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String username; // 管理员登录用户名

    @JsonIgnore
    @Column(nullable = false, length = 255)
    private String password; // 加密后的登录密码

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();

    @Column(name = "last_login")
    private LocalDateTime lastLogin; // 最后登录时间

    @JsonIgnore
    @Column(name = "access_token", length = 512)
    private String accessToken; // JWT访问令牌

    @JsonIgnore
    @Column(name = "refresh_token", length = 512)
    private String refreshToken; // JWT刷新令牌

    @JsonIgnore
    @Column(name = "totp_secret", length = 255)
    private String totpSecret; // TOTP双因素认证密钥

    @Column(name = "totp_enabled")
    private Boolean totpEnabled = false; // 是否启用TOTP双因素认证

    @Column(length = 100)
    private String email; // 管理员邮箱

    @JsonIgnore
    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;

    @JsonIgnore
    @Column(name = "lock_time")
    private LocalDateTime lockTime;

    @JsonIgnore
    @Column(name = "totp_recovery_codes", columnDefinition = "TEXT")
    private String totpRecoveryCodes; // TOTP恢复码（JSON数组，每个码经过SHA-256哈希）

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Admin that = (Admin) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
