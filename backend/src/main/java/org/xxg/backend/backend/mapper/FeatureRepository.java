package org.xxg.backend.backend.mapper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.xxg.backend.backend.entity.Feature;
import java.util.List;

/**
 * 系统特性数据访问接口
 * 提供系统特性配置的增删改查及按状态排序查询功能
 */
public interface FeatureRepository extends JpaRepository<Feature, Integer> {
    /** 查询所有启用的特性，按排序顺序升序排列 */
    List<Feature> findByStatusTrueOrderBySortOrderAsc();
}
