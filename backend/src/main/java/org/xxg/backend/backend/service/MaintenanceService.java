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
    @Transactional(readOnly = true)
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
        // 邮件主题也需净化，防止注入
        if (settings.getEmailSubject() != null) {
            settings.setEmailSubject(settings.getEmailSubject().replaceAll("[<>\"']", "").trim());
        }
        return repository.save(settings);
    }

    /**
     * HTML 净化器：移除危险元素，仅保留安全的格式化标签。
     * <p>采用黑名单+白名单双重策略：</p>
     * <ul>
     *   <li>移除所有危险标签及其内容（script/style/iframe/object/embed/form/base/meta/link）</li>
     *   <li>移除所有 on* 事件处理器（含无引号、大小写混合变体）</li>
     *   <li>移除 javascript:/data:/vbscript: 协议 URL</li>
     *   <li>移除 HTML 注释（可包含 IE 条件表达式）</li>
     * </ul>
     */
    private String sanitizeHtml(String html) {
        if (html == null) return null;
        // 1. 移除危险标签及其内容
        html = html.replaceAll("(?i)<(script|style|iframe|object|embed|applet|form|base|meta|link|svg|math)[^>]*>[\\s\\S]*?</\\1>", "");
        // 2. 移除自闭合的危险标签
        html = html.replaceAll("(?i)<(script|style|iframe|object|embed|applet|base|meta|link|svg|math)[^>]*/?>", "");
        // 3. 移除 on* 事件处理器（支持无引号、单引号、双引号、有无空格等变体）
        html = html.replaceAll("(?i)\\s*on\\w+\\s*=\\s*(\"[^\"]*\"|'[^']*'|\\S+)", "");
        // 4. 移除 javascript:/data:/vbscript: 协议（在 href/src/action 等属性中）
        html = html.replaceAll("(?i)(href|src|action|formaction|data|codebase)\\s*=\\s*\"\\s*(javascript|data|vbscript)[^\"]*\"", "");
        html = html.replaceAll("(?i)(href|src|action|formaction|data|codebase)\\s*=\\s*'\\s*(javascript|data|vbscript)[^']*'", "");
        html = html.replaceAll("(?i)(href|src|action|formaction|data|codebase)\\s*=\\s*(javascript|data|vbscript)\\S*", "");
        // 5. 移除独立的 javascript:/data:/vbscript: URL
        html = html.replaceAll("(?i)(javascript|data|vbscript)\\s*:", "");
        // 6. 移除 HTML 注释
        html = html.replaceAll("<!--[\\s\\S]*?-->", "");
        return html;
    }

    /**
     * 检查系统是否处于维护模式
     * @return 维护模式开启返回true，否则返回false
     */
    @Transactional(readOnly = true)
    public boolean isMaintenanceMode() {
        Boolean enabled = getSettings().getEnabled();
        return Boolean.TRUE.equals(enabled);
    }
}
