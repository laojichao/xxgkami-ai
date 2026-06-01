package org.xxg.backend.backend.mapper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.xxg.backend.backend.entity.ApiKeyMachineSpecRedemption;
import java.util.Optional;

/**
 * API密钥机器码兑换记录数据访问接口
 * 提供API密钥与机器码规格兑换记录的增删改查及唯一性校验功能
 */
public interface ApiKeyMachineSpecRedemptionRepository extends JpaRepository<ApiKeyMachineSpecRedemption, Long> {
    /** 根据API密钥ID、机器码和规格键查询兑换记录 */
    Optional<ApiKeyMachineSpecRedemption> findByApiKeyIdAndMachineCodeAndSpecKey(
            Long apiKeyId, String machineCode, String specKey);
    /** 判断指定API密钥、机器码和规格键的兑换记录是否存在 */
    boolean existsByApiKeyIdAndMachineCodeAndSpecKey(Long apiKeyId, String machineCode, String specKey);
}
