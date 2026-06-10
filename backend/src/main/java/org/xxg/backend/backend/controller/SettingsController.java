package org.xxg.backend.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xxg.backend.backend.dto.ApiResponse;
import org.xxg.backend.backend.service.SettingsService;
import org.xxg.backend.backend.service.EmailService;

import java.util.Map;

/**
 * 系统设置接口控制器
 * <p>提供系统配置的查询和更新功能，包括支付配置、邮件配置等。</p>
 * <p>基础路径：/settings</p>
 * <p>权限：查询接口公开访问，更新接口仅管理员</p>
 */
@RestController
@RequestMapping("/settings")
public class SettingsController {
    private final SettingsService settingsService;
    private final EmailService emailService;

    public SettingsController(SettingsService settingsService, EmailService emailService) {
        this.settingsService = settingsService;
        this.emailService = emailService;
    }

    /**
     * @deprecated 此端点与 GET /settings/all 功能相同，请使用 GET /settings/all 替代
     */
    @Deprecated(since = "2026-06-10", forRemoval = false)
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> getAll() {
        return getAllSettings();
    }

    // 敏感字段关键字列表 — 包含这些关键字的配置项都会被脱敏（白名单模式，新增配置自动受保护）
    private static final String[] SENSITIVE_KEYWORDS = {
        "key", "secret", "password", "token", "credential", "private"
    };

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<Map<String, String>>> getAllSettings() {
        Map<String, String> settings = settingsService.getAllSettings();
        // 脱敏处理：遍历所有配置项，包含敏感关键字的字段自动脱敏
        for (Map.Entry<String, String> entry : settings.entrySet()) {
            if (isSensitiveKey(entry.getKey())) {
                entry.setValue(maskValue(entry.getValue()));
            }
        }
        return ResponseEntity.ok(ApiResponse.ok(settings));
    }

    /** 判断配置项名称是否包含敏感关键字（不区分大小写） */
    private boolean isSensitiveKey(String key) {
        String lowerKey = key.toLowerCase();
        for (String keyword : SENSITIVE_KEYWORDS) {
            if (lowerKey.contains(keyword)) return true;
        }
        return false;
    }

    /** 对敏感值进行脱敏：保留首尾各2字符，中间用****替代 */
    private String maskValue(String value) {
        if (value == null || value.isEmpty()) return value;
        if (value.length() <= 4) return "****";
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }

    /**
     * @deprecated 此端点与 POST /settings/save 功能相同，请使用 POST /settings/save 替代
     */
    @Deprecated(since = "2026-06-10", forRemoval = false)
    @PutMapping
    public ResponseEntity<ApiResponse<Void>> update(@RequestBody Map<String, String> settings) {
        settingsService.updateSettings(settings);
        return ResponseEntity.ok(ApiResponse.ok("设置已更新"));
    }

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<Void>> saveSettings(@RequestBody Map<String, String> settings) {
        settingsService.updateSettings(settings);
        return ResponseEntity.ok(ApiResponse.ok("设置已保存"));
    }

    /**
     * 手动刷新配置缓存
     * 当数据库配置被外部修改后，可通过此接口强制重新加载缓存
     */
    @PostMapping("/refresh-cache")
    public ResponseEntity<ApiResponse<Void>> refreshCache() {
        settingsService.refreshCache();
        return ResponseEntity.ok(ApiResponse.ok("配置缓存已刷新"));
    }

    @PostMapping("/email/test")
    public ResponseEntity<ApiResponse<Void>> sendTestEmail(@RequestBody Map<String, String> body) {
        try {
            emailService.sendVerificationCode(body.get("to"), "123456", "test");
            return ResponseEntity.ok(ApiResponse.ok("测试邮件已发送"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("发送失败，请检查邮件配置"));
        }
    }
}
