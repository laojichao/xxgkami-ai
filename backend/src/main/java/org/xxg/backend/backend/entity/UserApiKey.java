package org.xxg.backend.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户-API密钥关联实体 - 记录用户被分配的API密钥(多对多关系)
 */
@Data
@Entity
@Table(name = "user_api_keys")
public class UserApiKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId; // 用户ID

    @Column(name = "api_key_id", nullable = false)
    private Long apiKeyId; // API密钥ID

    @Column(name = "assign_time")
    private LocalDateTime assignTime = LocalDateTime.now(); // 分配时间
}
