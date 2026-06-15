package org.xxg.backend.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xxg.backend.backend.entity.Setting;
import org.xxg.backend.backend.mapper.SettingRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统设置服务
 * 管理系统配置项的读取和更新，支持单个和批量操作
 * 使用 ConcurrentHashMap 缓存配置项，减少对数据库的频繁查询
 */
@Service
public class SettingsService {

    private static final Logger log = LoggerFactory.getLogger(SettingsService.class);
    private final SettingRepository settingRepository;

    /** 本地缓存，避免频繁查询数据库 */
    private final Map<String, String> settingsCache = new ConcurrentHashMap<>();

    /** 缓存是否已加载标记 */
    private volatile boolean cacheLoaded = false;

    public SettingsService(SettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    /**
     * 获取指定配置项的值
     * 优先从本地缓存读取，缓存未命中时查询数据库并回填缓存
     * @param name 配置项名称
     * @param defaultValue 默认值
     * @return 配置项值，不存在返回默认值
     */
    @Transactional(readOnly = true)
    public String getValue(String name, String defaultValue) {
        // 先从缓存读取
        String cached = settingsCache.get(name);
        if (cached != null) return cached;
        // 缓存未命中，查数据库并回填缓存
        String value = settingRepository.findByName(name)
                .map(Setting::getValue)
                .orElse(defaultValue);
        settingsCache.put(name, value);
        return value;
    }

    /**
     * 获取所有系统配置项
     * 首次调用时从数据库加载全部配置到缓存，后续直接返回缓存副本
     * @return 配置项名称到值的映射Map（防御性拷贝）
     */
    @Transactional(readOnly = true)
    public Map<String, String> getAllSettings() {
        if (!cacheLoaded) {
            refreshCache();
        }
        return new HashMap<>(settingsCache);
    }

    /**
     * 更新单个配置项，不存在则新建
     * 更新数据库后同步刷新本地缓存
     * @param name 配置项名称
     * @param value 配置项值
     */
    @Transactional
    public void updateSetting(String name, String value) {
        Setting setting = settingRepository.findByName(name)
                .orElse(new Setting());
        setting.setName(name);
        setting.setValue(value);
        settingRepository.save(setting);
        // 同步更新缓存
        settingsCache.put(name, value);
    }

    /**
     * 批量更新配置项。
     * <p>一次性查询所有已存在的配置项，批量更新后统一 saveAll()，
     * 减少数据库往返次数。</p>
     *
     * @param settings 配置项名称到值的映射Map
     */
    @Transactional
    public void updateSettings(Map<String, String> settings) {
        if (settings == null || settings.isEmpty()) {
            return;
        }

        // 一次性查询所有已存在的配置项，按名称索引
        List<Setting> existingList = settingRepository.findAll();
        Map<String, Setting> existingMap = new HashMap<>(existingList.size());
        for (Setting s : existingList) {
            existingMap.put(s.getName(), s);
        }

        List<Setting> toSave = new ArrayList<>();
        for (Map.Entry<String, String> entry : settings.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            Setting setting = existingMap.getOrDefault(name, new Setting());
            setting.setName(name);
            setting.setValue(value);
            toSave.add(setting);
            // 同步更新缓存
            settingsCache.put(name, value);
        }

        settingRepository.saveAll(toSave);
    }

    /**
     * 刷新缓存，从数据库重新加载所有配置项
     * 可在管理后台手动调用，或在初始化时自动调用
     */
    public void refreshCache() {
        Map<String, String> newCache = new ConcurrentHashMap<>();
        settingRepository.findAll().forEach(s -> newCache.put(s.getName(), s.getValue()));
        settingsCache.clear();
        settingsCache.putAll(newCache);
        cacheLoaded = true;
    }

    /**
     * 定时刷新配置缓存，每 5 分钟执行一次。
     * <p>确保多实例部署时配置变更能及时生效，
     * 即使管理员未手动调用 /settings/refresh-cache 接口。</p>
     */
    @Scheduled(fixedRate = 300000) // 每5分钟执行一次
    public void scheduledRefreshCache() {
        try {
            if (cacheLoaded) {
                refreshCache();
                log.debug("配置缓存定时刷新完成");
            }
        } catch (Exception e) {
            log.warn("配置缓存定时刷新失败: {}", e.getMessage());
        }
    }
}
