package org.xxg.backend.backend.mapper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.xxg.backend.backend.entity.Setting;
import java.util.Optional;

/**
 * 系统设置数据访问接口
 * 提供系统配置项的增删改查及按名称查询和唯一性校验功能
 */
public interface SettingRepository extends JpaRepository<Setting, Integer> {
    /** 根据配置名称查询设置项 */
    Optional<Setting> findByName(String name);
    /** 判断指定名称的配置项是否存在 */
    boolean existsByName(String name);
}
