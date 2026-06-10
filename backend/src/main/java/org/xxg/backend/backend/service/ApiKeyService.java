package org.xxg.backend.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xxg.backend.backend.entity.ApiKey;
import org.xxg.backend.backend.exception.BusinessException;
import org.xxg.backend.backend.mapper.ApiKeyRepository;

import java.time.LocalDateTime;
import java.util.*;

/**
 * API密钥管理服务
 * 提供API密钥的创建、查询、更新、删除及验证功能
 */
@Service
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyService(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    /**
     * 创建新的API密钥
     * @param name 密钥名称
     * @param description 密钥描述
     * @return 创建成功的API密钥实体
     */
    @Transactional
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

    /**
     * 根据ID获取API密钥
     * @param id 密钥ID
     * @return API密钥实体，不存在则抛出异常
     */
    public ApiKey getApiKeyById(Integer id) {
        return apiKeyRepository.findById(id)
                .orElseThrow(() -> new BusinessException("API Key 不存在"));
    }

    /**
     * 根据密钥值查找API密钥
     * @param keyValue 密钥值
     * @return API密钥实体，不存在返回null
     */
    public ApiKey getApiKeyByValue(String keyValue) {
        return apiKeyRepository.findByKeyValue(keyValue).orElse(null);
    }

    /**
     * 获取所有API密钥列表
     * @return 所有API密钥列表
     */
    public List<ApiKey> getAllApiKeys() {
        return apiKeyRepository.findAll();
    }

    /**
     * 更新API密钥信息
     * @param id 密钥ID
     * @param name 新名称，为null则不更新
     * @param description 新描述，为null则不更新
     * @param status 新状态，为null则不更新
     * @return 更新后的API密钥实体
     */
    @Transactional
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

    /**
     * 删除指定API密钥
     * @param id 密钥ID
     */
    @Transactional
    public void deleteApiKey(Integer id) {
        apiKeyRepository.deleteById(id);
    }

    /**
     * 原子递增API密钥使用次数并更新最后使用时间
     * @param id 密钥ID
     */
    @Transactional
    public void incrementUseCount(Integer id) {
        apiKeyRepository.incrementUseCount(id, LocalDateTime.now());
    }

    /**
     * 验证API密钥是否有效
     * @param keyValue 密钥值
     * @return 密钥存在且状态为启用返回true，否则返回false
     */
    public boolean validateApiKey(String keyValue) {
        ApiKey apiKey = apiKeyRepository.findByKeyValue(keyValue).orElse(null);
        return apiKey != null && apiKey.getStatus();
    }
}
