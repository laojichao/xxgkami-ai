package org.xxg.backend.backend.mapper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.xxg.backend.backend.entity.MaintenanceSettings;

public interface MaintenanceSettingsRepository extends JpaRepository<MaintenanceSettings, Integer> {
}
