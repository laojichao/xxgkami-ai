package org.xxg.backend.backend.mapper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.xxg.backend.backend.entity.ApiKey;
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
}
