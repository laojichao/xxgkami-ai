package org.xxg.backend.backend.service;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
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
        // 使用 Jsoup Safelist 净化 HTML 字段，防止 XSS 攻击
        settings.setContent(sanitizeHtml(settings.getContent()));
        settings.setEmailTemplate(sanitizeHtml(settings.getEmailTemplate()));
        // 邮件主题为纯文本，移除 HTML 标签和特殊字符
        if (settings.getEmailSubject() != null) {
            settings.setEmailSubject(Jsoup.clean(settings.getEmailSubject(), Safelist.none()).trim());
        }
        return repository.save(settings);
    }

    /**
     * HTML 净化器：使用 Jsoup Safelist（白名单）机制，仅保留安全的格式化标签和属性。
     * <p>相比正则表达式，Jsoup 能更可靠地防止 XSS 绕过（如嵌套标签、编码绕过等）。</p>
     * <p>允许的标签：基础格式化标签（a/b/i/strong/em/br/p/div/span/ul/ol/li/h1-h6/table 等），
     * 允许的属性：href/src/style/title/alt 等，禁止 on* 事件处理器和 javascript: 协议。</p>
     *
     * @param html 待净化的 HTML 字符串
     * @return 净化后的安全 HTML，输入为 null 时返回 null
     */
    private String sanitizeHtml(String html) {
        if (html == null) return null;
        // 使用 relaxed Safelist：允许较丰富的格式化标签，但禁止脚本和危险属性
        return Jsoup.clean(html, Safelist.relaxed());
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
