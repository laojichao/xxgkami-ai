package org.xxg.backend.backend.service;

import org.springframework.stereotype.Service;
import org.xxg.backend.backend.entity.ApiKey;
import org.xxg.backend.backend.exception.BusinessException;
import org.xxg.backend.backend.mapper.ApiKeyRepository;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyService(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    public ApiKey createApiKey(String name, String description) {
        ApiKey apiKey = new ApiKey();
        apiKey.setKeyName(name);
        apiKey.setName(name);
        apiKey.setApiKeyValue(UUID.randomUUID().toString().replace("-", ""));
        apiKey.setKeyValue(UUID.randomUUID().toString());
        apiKey.setDescription(description);
        apiKey.setStatus(true);
        apiKey.setCreateTime(LocalDateTime.now());
        return apiKeyRepository.save(apiKey);
    }

    public ApiKey getApiKeyById(Integer id) {
        return apiKeyRepository.findById(id)
                .orElseThrow(() -> new BusinessException("API Key 不存在"));
    }

    public ApiKey getApiKeyByValue(String keyValue) {
        return apiKeyRepository.findByKeyValue(keyValue).orElse(null);
    }

    public List<ApiKey> getAllApiKeys() {
        return apiKeyRepository.findAll();
    }

    public ApiKey updateApiKey(Integer id, String name, String description, Boolean status) {
        ApiKey apiKey = getApiKeyById(id);
        if (name != null) {
            apiKey.setKeyName(name);
            apiKey.setName(name);
        }
        if (description != null) apiKey.setDescription(description);
        if (status != null) apiKey.setStatus(status);
        apiKey.setUpdateTime(LocalDateTime.now());
        return apiKeyRepository.save(apiKey);
    }

    public void deleteApiKey(Integer id) {
        apiKeyRepository.deleteById(id);
    }

    public void incrementUseCount(Integer id) {
        ApiKey apiKey = getApiKeyById(id);
        apiKey.setUseCount(apiKey.getUseCount() + 1);
        apiKey.setLastUseTime(LocalDateTime.now());
        apiKeyRepository.save(apiKey);
    }

    public boolean validateApiKey(String keyValue) {
        ApiKey apiKey = apiKeyRepository.findByKeyValue(keyValue).orElse(null);
        return apiKey != null && apiKey.getStatus();
    }
}
