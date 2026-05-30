package org.xxg.backend.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xxg.backend.backend.dto.ApiResponse;
import org.xxg.backend.backend.entity.ApiKey;
import org.xxg.backend.backend.service.ApiKeyService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/apikeys")
public class ApiKeyController {
    private final ApiKeyService apiKeyService;
    public ApiKeyController(ApiKeyService apiKeyService) { this.apiKeyService = apiKeyService; }

    @PostMapping
    public ResponseEntity<ApiResponse<ApiKey>> create(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok(apiKeyService.createApiKey(
                body.getOrDefault("name", "API Key"), body.get("description"))));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ApiKey>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(apiKeyService.getAllApiKeys()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ApiKey>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(apiKeyService.getApiKeyById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ApiKey>> update(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String description = (String) body.get("description");
        Boolean status = body.containsKey("status") ? (Boolean) body.get("status") : null;
        return ResponseEntity.ok(ApiResponse.ok(apiKeyService.updateApiKey(id, name, description, status)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        apiKeyService.deleteApiKey(id);
        return ResponseEntity.ok(ApiResponse.ok("API Key 已删除"));
    }

    @PostMapping("/{id}/users")
    public ResponseEntity<ApiResponse<Void>> assignUser(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        return ResponseEntity.status(501).body(ApiResponse.error("用户分配功能暂未实现"));
    }

    @DeleteMapping("/{id}/users/{userId}")
    public ResponseEntity<ApiResponse<Void>> unassignUser(@PathVariable Integer id, @PathVariable Integer userId) {
        return ResponseEntity.status(501).body(ApiResponse.error("取消分配功能暂未实现"));
    }
}
