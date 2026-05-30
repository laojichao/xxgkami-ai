package org.xxg.backend.backend.mapper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.xxg.backend.backend.entity.ApiKey;
import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Integer> {
    Optional<ApiKey> findByApiKeyValue(String apiKey);
    Optional<ApiKey> findByKeyValue(String keyValue);
    boolean existsByApiKeyValue(String apiKey);
    boolean existsByKeyValue(String keyValue);
}
