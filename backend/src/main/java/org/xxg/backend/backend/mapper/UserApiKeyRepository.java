package org.xxg.backend.backend.mapper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.xxg.backend.backend.entity.UserApiKey;
import java.util.List;
import java.util.Optional;

public interface UserApiKeyRepository extends JpaRepository<UserApiKey, Long> {
    List<UserApiKey> findByUserId(Long userId);
    Optional<UserApiKey> findByUserIdAndApiKeyId(Long userId, Long apiKeyId);
}
