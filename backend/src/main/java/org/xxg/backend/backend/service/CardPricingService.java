package org.xxg.backend.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xxg.backend.backend.entity.CardPricing;
import org.xxg.backend.backend.exception.BusinessException;
import org.xxg.backend.backend.mapper.CardPricingRepository;
import java.util.List;

/**
 * 卡密定价服务
 * 管理卡密的定价信息，支持按类型查询和增删改操作
 */
@Service
public class CardPricingService {
    private final CardPricingRepository repository;
    public CardPricingService(CardPricingRepository repository) { this.repository = repository; }

    /** 获取所有卡密定价列表 */
    public List<CardPricing> getAll() { return repository.findAll(); }

    /** 根据卡密类型查询定价列表 */
    public List<CardPricing> getByType(String type) { return repository.findByType(type); }

    /** 根据ID获取定价信息，不存在则抛出异常 */
    public CardPricing getById(Integer id) {
        return repository.findById(id).orElseThrow(() -> new BusinessException("定价不存在"));
    }

    /** 保存或更新卡密定价信息 */
    @Transactional
    public CardPricing save(CardPricing pricing) { return repository.save(pricing); }

    /** 根据ID删除卡密定价，不存在则抛出异常 */
    @Transactional
    public void delete(Integer id) {
        if (!repository.existsById(id)) {
            throw new BusinessException("定价不存在");
        }
        repository.deleteById(id);
    }
}
