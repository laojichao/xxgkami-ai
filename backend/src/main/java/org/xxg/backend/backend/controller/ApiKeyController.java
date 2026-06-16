package org.xxg.backend.backend.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.xxg.backend.backend.dto.ApiKeyCreateRequest;
import org.xxg.backend.backend.dto.ApiResponse;
import org.xxg.backend.backend.dto.UpdateApiKeyRequest;
import org.xxg.backend.backend.entity.ApiKey;
import org.xxg.backend.backend.entity.UserApiKey;
import org.xxg.backend.backend.exception.BusinessException;
import org.xxg.backend.backend.mapper.UserApiKeyRepository;
import org.xxg.backend.backend.service.ApiKeyService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * API密钥管理接口
 * <p>提供API Key的增删改查及用户分配功能，仅管理员可访问。</p>
 * <p>基础路径：/admin/apikeys</p>
 */
@RestController
@RequestMapping("/admin/apikeys")
public class ApiKeyController {
    private final ApiKeyService apiKeyService;
    private final UserApiKeyRepository userApiKeyRepository;
    public ApiKeyController(ApiKeyService apiKeyService, UserApiKeyRepository userApiKeyRepository) {
        this.apiKeyService = apiKeyService;
        this.userApiKeyRepository = userApiKeyRepository;
    }

    /**
     * 创建新的API Key
     * <p>POST /admin/apikeys</p>
     * <p>权限：管理员</p>
     * @param request 创建请求（含名称和描述）
     * @return 新创建的API Key信息（含完整密钥值，仅此一次返回）
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(@Valid @RequestBody ApiKeyCreateRequest request) {
        ApiKey apiKey = apiKeyService.createApiKey(request.getName(), request.getDescription(),
                request.getEnableCardEncryption(), request.getRequireMachineCode(),
                request.getWebhookConfig(), request.getMachineSpecOnceConfig());
        // 创建时返回完整密钥值（仅此一次，后续查询不再返回）
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("id", apiKey.getId());
        result.put("keyName", apiKey.getKeyName());
        result.put("name", apiKey.getName());
        result.put("apiKey", apiKey.getApiKeyValue());
        result.put("description", apiKey.getDescription());
        result.put("status", apiKey.getStatus());
        result.put("createTime", apiKey.getCreateTime());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 获取所有API Key列表
     * <p>GET /admin/apikeys</p>
     * <p>权限：管理员</p>
     * @return API Key列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ApiKey>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(apiKeyService.getAllApiKeys()));
    }

    /**
     * 根据ID获取单个API Key详情
     * <p>GET /admin/apikeys/{id}</p>
     * <p>权限：管理员</p>
     * @param id API Key的ID
     * @return 指定的API Key信息
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ApiKey>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(apiKeyService.getApiKeyById(id)));
    }

    /**
     * 更新API Key信息
     * <p>PUT /admin/apikeys/{id}</p>
     * <p>权限：管理员</p>
     * @param id API Key的ID
     * @param request 更新请求（含名称、描述、状态）
     * @return 更新后的API Key信息
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ApiKey>> update(@PathVariable Integer id,
                                                       @Valid @RequestBody UpdateApiKeyRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                apiKeyService.updateApiKey(id, request.getName(), request.getDescription(), request.getStatus(),
                        request.getEnableCardEncryption(), request.getRequireMachineCode(),
                        request.getWebhookConfig(), request.getMachineSpecOnceConfig())));
    }

    /**
     * 删除指定API Key
     * <p>DELETE /admin/apikeys/{id}</p>
     * <p>权限：管理员</p>
     * @param id API Key的ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        apiKeyService.deleteApiKey(id);
        return ResponseEntity.ok(ApiResponse.ok("API Key 已删除"));
    }

    /**
     * 将用户分配到指定API Key
     * <p>POST /admin/apikeys/{id}/users</p>
     * <p>权限：管理员</p>
     * @param id API Key的ID
     * @param body 请求体，包含 userId
     * @return 操作结果
     */
    @PostMapping("/{id}/users")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> assignUser(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        Long userId = body.get("userId") != null ? Long.valueOf(body.get("userId").toString()) : null;
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        // 检查 API Key 是否存在
        apiKeyService.getApiKeyById(id);
        // 检查是否已分配
        Optional<UserApiKey> existing = userApiKeyRepository.findByUserIdAndApiKeyId(userId, id.longValue());
        if (existing.isPresent()) {
            throw new BusinessException("该用户已分配此 API Key");
        }
        UserApiKey userApiKey = new UserApiKey();
        userApiKey.setUserId(userId);
        userApiKey.setApiKeyId(id.longValue());
        userApiKey.setAssignTime(LocalDateTime.now());
        userApiKeyRepository.save(userApiKey);
        return ResponseEntity.ok(ApiResponse.ok("用户分配成功"));
    }

    /**
     * 取消用户与API Key的关联
     * <p>DELETE /admin/apikeys/{id}/users/{userId}</p>
     * <p>权限：管理员</p>
     * @param id API Key的ID
     * @param userId 用户ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}/users/{userId}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> unassignUser(@PathVariable Integer id, @PathVariable Integer userId) {
        userApiKeyRepository.findByUserIdAndApiKeyId(userId.longValue(), id.longValue())
                .ifPresent(userApiKeyRepository::delete);
        return ResponseEntity.ok(ApiResponse.ok("用户取消分配成功"));
    }
}
