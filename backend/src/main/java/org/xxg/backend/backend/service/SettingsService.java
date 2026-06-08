package org.xxg.backend.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xxg.backend.backend.entity.Setting;
import org.xxg.backend.backend.mapper.SettingRepository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 系统设置服务
 * 管理系统配置项的读取和更新，支持单个和批量操作
 */
@Service
public class SettingsService {

    private final SettingRepository settingRepository;

    public SettingsService(SettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    /**
     * 获取指定配置项的值
     * @param name 配置项名称
     * @param defaultValue 默认值
     * @return 配置项值，不存在返回默认值
     */
    public String getValue(String name, String defaultValue) {
        return settingRepository.findByName(name)
                .map(Setting::getValue)
                .orElse(defaultValue);
    }

    /**
     * 获取所有系统配置项
     * @return 配置项名称到值的映射Map
     */
    public Map<String, String> getAllSettings() {
        return settingRepository.findAll().stream()
                .collect(Collectors.toMap(Setting::getName, Setting::getValue));
    }

    /**
     * 更新单个配置项，不存在则新建
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
}
