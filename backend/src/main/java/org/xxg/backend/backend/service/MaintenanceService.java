package org.xxg.backend.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xxg.backend.backend.entity.MaintenanceSettings;
import org.xxg.backend.backend.mapper.MaintenanceSettingsRepository;

@Service
public class MaintenanceService {
    private final MaintenanceSettingsRepository repository;
    public MaintenanceService(MaintenanceSettingsRepository repository) { this.repository = repository; }

    public MaintenanceSettings getSettings() {
        return repository.findById(1).orElse(new MaintenanceSettings());
    }

    @Transactional
    public MaintenanceSettings updateSettings(MaintenanceSettings settings) {
        settings.setId(1);
        return repository.save(settings);
    }

    public boolean isMaintenanceMode() {
        return getSettings().getEnabled();
    }
}
