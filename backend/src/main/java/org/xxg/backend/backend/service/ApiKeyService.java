package org.xxg.backend.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xxg.backend.backend.entity.ApiKey;
import org.xxg.backend.backend.exception.BusinessException;
import org.xxg.backend.backend.mapper.ApiKeyRepository;

import java.security.SecureRandom;
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
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * API 密钥随机字节数。
     * <p>固定为 16 字节（32 个十六进制字符），受 {@code api_keys.api_key} 列
     * 长度限制（VARCHAR(32)）。两个密钥字段存储相同的值，保证返回给管理员的
     * 密钥与库中用于认证的密钥完全一致。</p>
     */
    private static final int API_KEY_BYTES = 16;

    @Transactional
    public ApiKey createApiKey(String name, String description) {
        return createApiKey(name, description, null, null, null, null);
    }

    /**
     * 创建新的API密钥（支持完整参数）
     * @param name 密钥名称
     * @param description 密钥描述
     * @param enableCardEncryption 是否启用卡密加密
     * @param requireMachineCode 是否要求机器码
     * @param webhookConfig Webhook配置
     * @param machineSpecOnceConfig 机器码一次性配置
     * @return 创建成功的API密钥实体
     */
    @Transactional
    public ApiKey createApiKey(String name, String description, Boolean enableCardEncryption,
                               Boolean requireMachineCode, String webhookConfig, String machineSpecOnceConfig) {
        ApiKey apiKey = new ApiKey();
        apiKey.setKeyName(name);
        apiKey.setName(name);
        // 安全修复：统一使用一个密钥值，避免 apiKeyValue 与 keyValue 不一致导致认证混乱。
        // 两个字段写入完全相同的值：
        //   - key_value (255) 作为主存储，供 findByKeyValue 认证查询使用
        //   - api_key   (32)  受列长度限制，因此密钥长度固定为 32 个十六进制字符（128 bit 熵）
        // 历史缺陷：曾将 64 字符密钥的前 32 字符写入 api_key 并在创建接口返回，
        // 导致管理员拿到的密钥与库中存储的完整密钥不一致，任何调用都会认证失败。
        String unifiedKey = generateSecureKey(API_KEY_BYTES);
        apiKey.setKeyValue(unifiedKey);
        apiKey.setApiKeyValue(unifiedKey);
        apiKey.setDescription(description);
        apiKey.setStatus(true);
        apiKey.setCreateTime(LocalDateTime.now());
        if (enableCardEncryption != null) apiKey.setEnableCardEncryption(enableCardEncryption);
        if (requireMachineCode != null) apiKey.setRequireMachineCode(requireMachineCode);
        if (webhookConfig != null) apiKey.setWebhookConfig(webhookConfig);
        if (machineSpecOnceConfig != null) apiKey.setMachineSpecOnceConfig(machineSpecOnceConfig);
        return apiKeyRepository.save(apiKey);
    }

    /**
     * 生成指定字节数的安全随机密钥（十六进制格式）。
     * @param bytes 字节数（32 字节 = 256 位 = 64 个十六进制字符）
     */
    private String generateSecureKey(int bytes) {
        byte[] keyBytes = new byte[bytes];
        SECURE_RANDOM.nextBytes(keyBytes);
        StringBuilder sb = new StringBuilder(bytes * 2);
        for (byte b : keyBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 根据ID获取API密钥
     * @param id 密钥ID
     * @return API密钥实体，不存在则抛出异常
     */
    @Transactional(readOnly = true)
    public ApiKey getApiKeyById(Integer id) {
        return apiKeyRepository.findById(id)
                .orElseThrow(() -> new BusinessException("API Key 不存在"));
    }

    /**
     * 根据密钥值查找API密钥
     * @param keyValue 密钥值
     * @return API密钥实体，不存在返回null
     */
    @Transactional(readOnly = true)
    public ApiKey getApiKeyByValue(String keyValue) {
        return apiKeyRepository.findByKeyValue(keyValue).orElse(null);
    }

    /**
     * 获取所有API密钥列表
     * @return 所有API密钥列表
     */
    @Transactional(readOnly = true)
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
        return updateApiKey(id, name, description, status, null, null, null, null);
    }

    /**
     * 更新API密钥信息（支持完整参数）
     * @param id 密钥ID
     * @param name 新名称
     * @param description 新描述
     * @param status 新状态
     * @param enableCardEncryption 是否启用卡密加密
     * @param requireMachineCode 是否要求机器码
     * @param webhookConfig Webhook配置
     * @param machineSpecOnceConfig 机器码一次性配置
     * @return 更新后的API密钥实体
     */
    @Transactional
    public ApiKey updateApiKey(Integer id, String name, String description, Boolean status,
                                Boolean enableCardEncryption, Boolean requireMachineCode,
                                String webhookConfig, String machineSpecOnceConfig) {
        ApiKey apiKey = getApiKeyById(id);
        if (name != null) {
            apiKey.setKeyName(name);
            apiKey.setName(name);
        }
        if (description != null) apiKey.setDescription(description);
        if (status != null) apiKey.setStatus(status);
        if (enableCardEncryption != null) apiKey.setEnableCardEncryption(enableCardEncryption);
        if (requireMachineCode != null) apiKey.setRequireMachineCode(requireMachineCode);
        if (webhookConfig != null) apiKey.setWebhookConfig(webhookConfig);
        if (machineSpecOnceConfig != null) apiKey.setMachineSpecOnceConfig(machineSpecOnceConfig);
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
    @Transactional(readOnly = true)
    public boolean validateApiKey(String keyValue) {
        ApiKey apiKey = apiKeyRepository.findByKeyValue(keyValue).orElse(null);
        return apiKey != null && Boolean.TRUE.equals(apiKey.getStatus());
    }
}
