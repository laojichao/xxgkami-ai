package org.xxg.backend.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 用户-API密钥关联实体 - 记录用户被分配的API密钥(多对多关系)
 */
@Getter
@Setter
@Entity
@Table(name = "user_api_keys")
public class UserApiKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Integer userId; // 用户ID（与 User.id 类型一致）

    @Column(name = "api_key_id", nullable = false)
    private Integer apiKeyId; // API密钥ID（与 ApiKey.id 类型一致）

    @Column(name = "assign_time")
    private LocalDateTime assignTime = LocalDateTime.now(); // 分配时间

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserApiKey that = (UserApiKey) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
