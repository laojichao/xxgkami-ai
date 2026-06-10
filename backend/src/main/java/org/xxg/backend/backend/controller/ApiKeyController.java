package org.xxg.backend.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xxg.backend.backend.dto.ApiResponse;
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
     * @param body 请求体，包含 name（名称，默认"API Key"）和 description（描述）
     * @return 新创建的API Key信息
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ApiKey>> create(@RequestBody Map<String, String> body) {
        String name = body.getOrDefault("name", "API Key");
        if (name.length() > 100) {
            return ResponseEntity.badRequest().body(ApiResponse.error("API Key 名称不能超过100个字符"));
        }
        return ResponseEntity.ok(ApiResponse.ok(apiKeyService.createApiKey(name, body.get("description"))));
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
     * @param body 请求体，可包含 name、description、status 字段
     * @return 更新后的API Key信息
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ApiKey>> update(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String description = (String) body.get("description");
        Boolean status = body.containsKey("status") ? (Boolean) body.get("status") : null;
        return ResponseEntity.ok(ApiResponse.ok(apiKeyService.updateApiKey(id, name, description, status)));
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
