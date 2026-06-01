package org.xxg.backend.backend.mapper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.xxg.backend.backend.entity.MaintenanceSettings;

/**
 * 维护设置数据访问接口
 * 提供系统维护模式配置的增删改查功能
 */
public interface MaintenanceSettingsRepository extends JpaRepository<MaintenanceSettings, Integer> {
}
