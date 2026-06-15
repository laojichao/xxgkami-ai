package org.xxg.backend.backend.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xxg.backend.backend.dto.ApiKeyCreateRequest;
import org.xxg.backend.backend.dto.ApiResponse;
import org.xxg.backend.backend.dto.UpdateApiKeyRequest;
import org.xxg.backend.backend.entity.ApiKey;
import org.xxg.backend.backend.service.ApiKeyService;

import java.util.List;
import java.util.Map;

/**
 * API密钥管理接口
 * <p>提供API Key的增删改查及用户分配功能，仅管理员可访问。</p>
 * <p>基础路径：/admin/apikeys</p>
 */
@RestController
@RequestMapping("/admin/apikeys")
public class ApiKeyController {
    private final ApiKeyService apiKeyService;
    public ApiKeyController(ApiKeyService apiKeyService) { this.apiKeyService = apiKeyService; }

    /**
     * 创建新的API Key
     * <p>POST /admin/apikeys</p>
     * <p>权限：管理员</p>
     * @param request 创建请求（含名称和描述）
     * @return 新创建的API Key信息（含完整密钥值，仅此一次返回）
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(@Valid @RequestBody ApiKeyCreateRequest request) {
        ApiKey apiKey = apiKeyService.createApiKey(request.getName(), request.getDescription());
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
                apiKeyService.updateApiKey(id, request.getName(), request.getDescription(), request.getStatus())));
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
     * 将用户分配到指定API Key（暂未实现）
     * <p>POST /admin/apikeys/{id}/users</p>
     * <p>权限：管理员</p>
     * @param id API Key的ID
     * @param body 请求体，包含用户信息
     * @return 501 未实现
     */
    @PostMapping("/{id}/users")
    public ResponseEntity<ApiResponse<Void>> assignUser(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        return ResponseEntity.status(501).body(ApiResponse.error("用户分配功能暂未实现"));
    }

    /**
     * 取消用户与API Key的关联（暂未实现）
     * <p>DELETE /admin/apikeys/{id}/users/{userId}</p>
     * <p>权限：管理员</p>
     * @param id API Key的ID
     * @param userId 用户ID
     * @return 501 未实现
     */
    @DeleteMapping("/{id}/users/{userId}")
    public ResponseEntity<ApiResponse<Void>> unassignUser(@PathVariable Integer id, @PathVariable Integer userId) {
        return ResponseEntity.status(501).body(ApiResponse.error("取消分配功能暂未实现"));
    }
}
