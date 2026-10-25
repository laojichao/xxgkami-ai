package org.xxg.backend.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * 验证码实体 - 存储邮箱验证码信息，用于注册、找回密码等场景
 */
@Data
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "verification_codes", indexes = {
    @Index(name = "idx_vcode_email_type_time", columnList = "email, type, create_time")
})
public class VerificationCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String email; // 接收验证码的邮箱地址

    // 验证码内容（SHA-256 hex，64 字符）。历史缺陷：列宽 10 是明文 6 位码时代
    // 的遗留，第四轮改为哈希入库后未同步加宽——MySQL 严格模式下发码接口
    // 直接 Data too long 500，非严格模式静默截断导致验证码永远比对失败。
    // 注意 ddl-auto=validate 不会自动改表，存量库需执行迁移 SQL：
    // ALTER TABLE verification_codes MODIFY COLUMN code VARCHAR(64) NOT NULL;
    @Column(nullable = false, length = 64)
    private String code; // 验证码内容

    @Column(nullable = false, length = 20)
    private String type = "register"; // 验证码类型: register=注册, reset=重置密码

    @Column(name = "expire_time", nullable = false)
    private LocalDateTime expireTime; // 验证码过期时间

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();

    @Column(name = "attempts")
    private Integer attempts = 0;
}
