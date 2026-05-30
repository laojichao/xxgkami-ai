package org.xxg.backend.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xxg.backend.backend.dto.ApiResponse;
import org.xxg.backend.backend.service.SettingsService;
import org.xxg.backend.backend.service.EmailService;

import java.util.Map;

@RestController
@RequestMapping("/settings")
public class SettingsController {
    private final SettingsService settingsService;
    private final EmailService emailService;

    public SettingsController(SettingsService settingsService, EmailService emailService) {
        this.settingsService = settingsService;
        this.emailService = emailService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(settingsService.getAllSettings()));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<Map<String, String>>> getAllSettings() {
        Map<String, String> settings = settingsService.getAllSettings();
        // Mask sensitive fields
        maskSensitiveField(settings, "epay_key");
        maskSensitiveField(settings, "mail_password");
        maskSensitiveField(settings, "smtp_password");
        maskSensitiveField(settings, "secret");
        return ResponseEntity.ok(ApiResponse.ok(settings));
    }

    private void maskSensitiveField(Map<String, String> settings, String key) {
        String value = settings.get(key);
        if (value != null && !value.isEmpty() && value.length() > 4) {
            settings.put(key, value.substring(0, 2) + "****" + value.substring(value.length() - 2));
        }
    }

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
