package org.xxg.backend.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * 社交账号绑定实体 - 记录用户与第三方社交账号的绑定关系
 */
@Data
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "social_users")
public class SocialUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId; // 关联的系统用户ID

    @Column(name = "social_uid", nullable = false, length = 100)
    private String socialUid; // 第三方平台用户唯一标识

    @Column(name = "social_type", nullable = false, length = 20)
    private String socialType; // 社交平台类型(如: wechat, qq, github等)

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();
}
