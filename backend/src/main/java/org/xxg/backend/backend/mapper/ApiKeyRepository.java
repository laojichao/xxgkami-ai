package org.xxg.backend.backend.mapper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.xxg.backend.backend.entity.ApiKey;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * API密钥数据访问接口
 * 提供API密钥的增删改查及按密钥值查询和唯一性校验功能
 */
public interface ApiKeyRepository extends JpaRepository<ApiKey, Integer> {
    /** 根据API密钥值查询密钥 */
    Optional<ApiKey> findByApiKeyValue(String apiKey);
    /** 根据键值查询密钥 */
    Optional<ApiKey> findByKeyValue(String keyValue);
    /** 判断指定API密钥值是否存在 */
    boolean existsByApiKeyValue(String apiKey);
    /** 判断指定键值是否存在 */
    boolean existsByKeyValue(String keyValue);

    /**
     * 原子递增API密钥使用次数并更新最后使用时间
     * @param id 密钥ID
     * @param now 当前时间
     * @return 受影响的行数（0表示密钥不存在）
     */
    @Modifying
    @Query("UPDATE ApiKey a SET a.useCount = a.useCount + 1, a.lastUseTime = :now WHERE a.id = :id")
    int incrementUseCount(@Param("id") Integer id, @Param("now") LocalDateTime now);
}
