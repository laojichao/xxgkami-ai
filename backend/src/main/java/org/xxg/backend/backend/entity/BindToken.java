package org.xxg.backend.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 绑定令牌实体 - 存储第三方登录绑定场景中的一次性绑定令牌
 * <p>令牌特性:</p>
 * <ul>
 *   <li>生成后 10 分钟内有效</li>
 *   <li>验证成功后标记为已使用 (used=true)，不可重复使用</li>
 *   <li>每个令牌绑定到特定用户，防止跨用户滥用</li>
 * </ul>
 */
@Data
@Entity
@Table(name = "bind_tokens")
public class BindToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** 令牌内容 (UUID) */
    @Column(nullable = false, unique = true, length = 64)
    private String token;

    /** 生成该令牌的用户 ID */
    @Column(name = "user_id")
    private Integer userId;

    /** 令牌过期时间 */
    @Column(name = "expire_time", nullable = false)
    private LocalDateTime expireTime;

    /** 是否已使用 */
    @Column(name = "used", nullable = false)
    private Boolean used = false;

    /** 创建时间 */
    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();
}
