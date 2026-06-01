package org.xxg.backend.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户实体 - 系统注册用户的基本信息和认证数据
 */
@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String username; // 登录用户名(唯一)

    @Column(unique = true, length = 100)
    private String email; // 邮箱地址(唯一, 用于找回密码等)

    @Column(nullable = false, length = 255)
    private String password; // 加密后的登录密码

    @Column(length = 50)
    private String nickname; // 用户昵称(显示名称)

    @Column(length = 255)
    private String avatar; // 头像URL地址

    @Column(length = 20)
    private String phone; // 手机号码

    private Boolean status = true; // 账号状态(true=正常, false=禁用)

    @Column(name = "email_verified")
    private Boolean emailVerified = false; // 邮箱是否已验证

    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime; // 最后登录时间

    @Column(name = "last_login_ip", length = 50)
    private String lastLoginIp; // 最后登录IP地址

    @Column(name = "login_count")
    private Integer loginCount = 0; // 累计登录次数

    @Column(name = "register_ip", length = 50)
    private String registerIp; // 注册时的IP地址

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();

    @Column(name = "update_time")
    private LocalDateTime updateTime = LocalDateTime.now();

    @Column(name = "access_token", length = 512)
    private String accessToken; // JWT访问令牌

    @Column(name = "refresh_token", length = 512)
    private String refreshToken; // JWT刷新令牌
}
