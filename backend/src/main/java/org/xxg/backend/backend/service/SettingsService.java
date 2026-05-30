package org.xxg.backend.backend.service;

import org.springframework.stereotype.Service;
import org.xxg.backend.backend.entity.Setting;
import org.xxg.backend.backend.mapper.SettingRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SettingsService {

    private final SettingRepository settingRepository;

    public SettingsService(SettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    public String getValue(String name, String defaultValue) {
        return settingRepository.findByName(name)
                .map(Setting::getValue)
                .orElse(defaultValue);
    }

    public Map<String, String> getAllSettings() {
        return settingRepository.findAll().stream()
                .collect(Collectors.toMap(Setting::getName, Setting::getValue));
    }

    public void updateSetting(String name, String value) {
        Setting setting = settingRepository.findByName(name)
                .orElse(new Setting());
        setting.setName(name);
        setting.setValue(value);
        settingRepository.save(setting);
    }

    public void updateSettings(Map<String, String> settings) {
        for (Map.Entry<String, String> entry : settings.entrySet()) {
            updateSetting(entry.getKey(), entry.getValue());
        }
    }
}
