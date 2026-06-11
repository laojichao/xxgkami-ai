package org.xxg.backend.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xxg.backend.backend.dto.ApiResponse;
import org.xxg.backend.backend.service.SettingsService;
import org.xxg.backend.backend.service.EmailService;

import java.util.Map;
import java.util.Set;

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

    // 禁止修改的敏感配置键（防止通过设置接口篡改核心安全配置，值全部小写用于比较）
    private static final Set<String> BLOCKED_KEYS = Set.of(
        "jwt.secret", "jwt_secret",
        "spring.datasource.password", "db.password", "db_password",
        "spring.datasource.username", "db.username", "db_username"
    );

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

    /** 对敏感值进行脱敏：长度<=8时完全隐藏，>8时仅保留首尾各2字符 */
    private String maskValue(String value) {
        if (value == null || value.isEmpty()) return value;
        if (value.length() <= 8) return "********";
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }

    /**
     * 检查值是否为脱敏格式（防止前端未修改的脱敏值被回写覆盖真实配置）。
     * <p>匹配模式：全星号 或 XX****XX（首尾各2字符+中间4个星号）</p>
     */
    private boolean isMaskedValue(String value) {
        if (value == null) return false;
        // 全星号模式：********
        if (value.matches("^\\*{4,}$")) return true;
        // 首尾各2字符+****模式：ab****cd
        if (value.length() == 8 && value.substring(2, 6).equals("****")
                && !value.substring(0, 2).contains("*") && !value.substring(6).contains("*")) {
            return true;
        }
        return false;
    }

    /**
     * 过滤掉脱敏值，避免前端未修改的脱敏字段覆盖数据库中的真实配置。
     * <p>前端获取设置时，敏感字段会被脱敏后返回。如果用户未修改这些字段就点击保存，
     * 脱敏值（如 "********"）会被回传。此方法识别并跳过这些值。</p>
     */
    private Map<String, String> filterMaskedValues(Map<String, String> settings) {
        Map<String, String> filtered = new java.util.HashMap<>();
        for (Map.Entry<String, String> entry : settings.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            // 仅对敏感字段检查脱敏值，非敏感字段直接放行
            if (isSensitiveKey(key) && isMaskedValue(value)) {
                continue; // 跳过脱敏值，不更新此字段
            }
            filtered.put(key, value);
        }
        return filtered;
    }

    /**
     * @deprecated 此端点与 POST /settings/save 功能相同，请使用 POST /settings/save 替代
     */
    @Deprecated(since = "2026-06-10", forRemoval = false)
    @PutMapping
    public ResponseEntity<ApiResponse<Void>> update(@RequestBody Map<String, String> settings) {
        for (String key : settings.keySet()) {
            if (BLOCKED_KEYS.contains(key.toLowerCase())) {
                return ResponseEntity.ok(ApiResponse.error("不允许修改敏感配置项: " + key));
            }
        }
        Map<String, String> filtered = filterMaskedValues(settings);
        settingsService.updateSettings(filtered);
        return ResponseEntity.ok(ApiResponse.ok("设置已更新"));
    }

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<Void>> saveSettings(@RequestBody Map<String, String> settings) {
        for (String key : settings.keySet()) {
            if (BLOCKED_KEYS.contains(key.toLowerCase())) {
                return ResponseEntity.ok(ApiResponse.error("不允许修改敏感配置项: " + key));
            }
        }
        Map<String, String> filtered = filterMaskedValues(settings);
        settingsService.updateSettings(filtered);
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
