package org.xxg.backend.backend.mapper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.xxg.backend.backend.entity.Slide;
import java.util.List;

/**
 * 轮播图数据访问接口
 * 提供轮播图的增删改查及按状态排序查询功能
 */
public interface SlideRepository extends JpaRepository<Slide, Integer> {
    /** 查询所有启用的轮播图，按排序顺序升序排列 */
    List<Slide> findByStatusTrueOrderBySortOrderAsc();
}
