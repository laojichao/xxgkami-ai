package org.xxg.backend.backend.mapper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    /** 根据API密钥ID查询绑定该密钥的用户ID列表（列表装配/级联清理用） */
    @Query("SELECT u.userId FROM UserApiKey u WHERE u.apiKeyId = :apiKeyId")
    List<Integer> findUserIdsByApiKeyId(@Param("apiKeyId") Integer apiKeyId);
    /** 根据用户ID删除全部绑定记录（删除用户时级联清理） */
    void deleteByUserId(Integer userId);
    /** 根据API密钥ID删除全部绑定记录（删除API密钥时级联清理） */
    void deleteByApiKeyId(Integer apiKeyId);
    /** 根据用户ID和API密钥ID查询绑定记录 */
    Optional<UserApiKey> findByUserIdAndApiKeyId(Integer userId, Integer apiKeyId);
}
