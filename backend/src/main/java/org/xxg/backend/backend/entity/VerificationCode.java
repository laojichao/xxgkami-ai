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

    @Column(nullable = false, length = 10)
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
