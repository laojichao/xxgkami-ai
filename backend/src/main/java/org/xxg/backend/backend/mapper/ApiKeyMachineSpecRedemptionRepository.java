package org.xxg.backend.backend.mapper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.xxg.backend.backend.entity.ApiKeyMachineSpecRedemption;
import java.util.Optional;

public interface ApiKeyMachineSpecRedemptionRepository extends JpaRepository<ApiKeyMachineSpecRedemption, Long> {
    Optional<ApiKeyMachineSpecRedemption> findByApiKeyIdAndMachineCodeAndSpecKey(
            Long apiKeyId, String machineCode, String specKey);
    boolean existsByApiKeyIdAndMachineCodeAndSpecKey(Long apiKeyId, String machineCode, String specKey);
}
