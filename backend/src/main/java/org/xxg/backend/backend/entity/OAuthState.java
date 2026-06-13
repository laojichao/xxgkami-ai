package org.xxg.backend.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * OAuth state 实体 - 存储 OAuth 登录流程中的一次性 state nonce。
 * <p>用于防止 OAuth session fixation 攻击：</p>
 * <ul>
 *   <li>前端发起 OAuth 前调用 /auth/oauth/state 获取 state</li>
 *   <li>回调时必须携带此 state 才能设置 Cookie</li>
 *   <li>使用后自动失效（一次性令牌）</li>
 *   <li>5 分钟过期</li>
 * </ul>
 * <p>相比内存存储，数据库持久化支持多实例部署和应用重启。</p>
 */
@Data
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "oauth_states", indexes = {
    @Index(name = "idx_oauth_state_token", columnList = "state")
})
public class OAuthState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** state nonce (UUID) */
    @Column(nullable = false, unique = true, length = 64)
    private String state;

    /** 过期时间 */
    @Column(name = "expire_time", nullable = false)
    private LocalDateTime expireTime;

    /** 创建时间 */
    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();
}
