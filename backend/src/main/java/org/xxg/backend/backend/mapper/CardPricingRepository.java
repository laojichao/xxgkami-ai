package org.xxg.backend.backend.mapper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.xxg.backend.backend.entity.CardPricing;
import java.util.List;
import java.util.Optional;

/**
 * 卡密定价数据访问接口
 * 提供卡密定价规则的增删改查及按类型和面值查询功能
 */
public interface CardPricingRepository extends JpaRepository<CardPricing, Integer> {
    /** 根据卡密类型查询定价列表 */
    List<CardPricing> findByType(String type);
    /** 根据卡密类型和面值查询定价 */
    Optional<CardPricing> findByTypeAndValue(String type, Integer value);
}
