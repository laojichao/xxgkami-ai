package org.xxg.backend.backend.mapper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.xxg.backend.backend.entity.UserApiKey;
import java.util.List;
import java.util.Optional;

/**
 * 用户API密钥关联数据访问接口
 * 提供用户与API密钥绑定关系的增删改查功能
 */
public interface UserApiKeyRepository extends JpaRepository<UserApiKey, Long> {
    /** 根据用户ID查询其绑定的API密钥列表 */
    List<UserApiKey> findByUserId(Integer userId);
    /** 根据用户ID和API密钥ID查询绑定记录 */
    Optional<UserApiKey> findByUserIdAndApiKeyId(Integer userId, Integer apiKeyId);
}
