package org.xxg.backend.backend.service;

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
    public String getValue(String name, String defaultValue) {
        // 先从缓存读取
        if (settingsCache.containsKey(name)) {
            return settingsCache.get(name);
        }
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
     * 批量更新配置项
     * @param settings 配置项名称到值的映射Map
     */
    @Transactional
    public void updateSettings(Map<String, String> settings) {
        for (Map.Entry<String, String> entry : settings.entrySet()) {
            updateSetting(entry.getKey(), entry.getValue());
        }
    }

    /**
     * 刷新缓存，从数据库重新加载所有配置项
     * 可在管理后台手动调用，或在初始化时自动调用
     */
    public void refreshCache() {
        settingsCache.clear();
        settingRepository.findAll().forEach(s -> settingsCache.put(s.getName(), s.getValue()));
        cacheLoaded = true;
    }
}
