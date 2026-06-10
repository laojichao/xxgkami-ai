package org.xxg.backend.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xxg.backend.backend.entity.MaintenanceSettings;
import org.xxg.backend.backend.mapper.MaintenanceSettingsRepository;

/**
 * 系统维护服务
 * 管理系统维护模式的开启和关闭，以及维护状态的查询
 */
@Service
public class MaintenanceService {
    private final MaintenanceSettingsRepository repository;
    public MaintenanceService(MaintenanceSettingsRepository repository) { this.repository = repository; }

    /** 获取系统维护设置，不存在则返回默认设置 */
    public MaintenanceSettings getSettings() {
        return repository.findById(1).orElse(new MaintenanceSettings());
    }

    /**
     * 更新系统维护设置
     * @param settings 维护设置实体
     * @return 更新后的设置
     */
    @Transactional
    public MaintenanceSettings updateSettings(MaintenanceSettings settings) {
        settings.setId(1);
        // Sanitize HTML fields to remove potentially dangerous content
        settings.setContent(sanitizeHtml(settings.getContent()));
        settings.setEmailTemplate(sanitizeHtml(settings.getEmailTemplate()));
        return repository.save(settings);
    }

    /**
     * Simple HTML sanitizer that removes dangerous elements while keeping safe formatting tags.
     * Removes script/style tags (with content), event handlers, and javascript: URLs.
     */
    private String sanitizeHtml(String html) {
        if (html == null) return null;
        // Remove script/style tags and their content
        html = html.replaceAll("(?i)<script[^>]*>[\\s\\S]*?</script>", "");
        html = html.replaceAll("(?i)<style[^>]*>[\\s\\S]*?</style>", "");
        // Remove on* event handlers
        html = html.replaceAll("(?i)\\s+on\\w+\\s*=\\s*[\"'][^\"']*[\"']", "");
        html = html.replaceAll("(?i)\\s+on\\w+\\s*=\\s*\\S+", "");
        // Remove javascript: URLs
        html = html.replaceAll("(?i)javascript\\s*:", "");
        return html;
    }

    /**
     * 检查系统是否处于维护模式
     * @return 维护模式开启返回true，否则返回false
     */
    public boolean isMaintenanceMode() {
        Boolean enabled = getSettings().getEnabled();
        return Boolean.TRUE.equals(enabled);
    }
}
