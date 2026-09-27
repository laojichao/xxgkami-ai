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

    /** 时长卡最大天数（约 10 年），防止出现不合理的超长有效期 */
    private static final int MAX_TIME_CARD_DAYS = 3650;
    /** 次数卡最大次数 */
    private static final int MAX_COUNT_CARD_TIMES = 1_000_000;

    private final CardPricingRepository repository;
    public CardPricingService(CardPricingRepository repository) { this.repository = repository; }

    /** 获取所有卡密定价列表 */
    @Transactional(readOnly = true)
    public List<CardPricing> getAll() { return repository.findAll(); }

    /** 根据卡密类型查询定价列表 */
    @Transactional(readOnly = true)
    public List<CardPricing> getByType(String type) { return repository.findByType(type); }

    /** 根据ID获取定价信息，不存在则抛出异常 */
    @Transactional(readOnly = true)
    public CardPricing getById(Integer id) {
        return repository.findById(id).orElseThrow(() -> new BusinessException("定价不存在"));
    }

    /**
     * 保存或更新卡密定价信息。
     * <p>定价的 type 与 value 是订单金额和卡密规格的唯一权威来源
     * （{@code OrderService.createOrder} 依据 pricing.value 推导卡密天数/次数），
     * 因此必须在写入前校验，避免出现无法下单或规格异常的脏数据。</p>
     *
     * @param pricing 定价实体
     * @return 保存后的定价实体
     * @throws BusinessException 当 type 或 value 非法时抛出
     */
    @Transactional
    public CardPricing save(CardPricing pricing) {
        String type = pricing.getType();
        if (!"time".equals(type) && !"count".equals(type)) {
            throw new BusinessException("无效的卡密类型，仅支持 time 或 count");
        }
        Integer value = pricing.getValue();
        if (value == null || value <= 0) {
            throw new BusinessException("规格值必须大于0");
        }
        // 与 Card.duration（天数）字段保持一致，避免超出 int 范围或产生不合理的超长有效期
        if ("time".equals(type) && value > MAX_TIME_CARD_DAYS) {
            throw new BusinessException("时长卡天数不能超过 " + MAX_TIME_CARD_DAYS + " 天");
        }
        if ("count".equals(type) && value > MAX_COUNT_CARD_TIMES) {
            throw new BusinessException("次数卡次数不能超过 " + MAX_COUNT_CARD_TIMES + " 次");
        }
        return repository.save(pricing);
    }

    /** 根据ID删除卡密定价，不存在则抛出异常 */
    @Transactional
    public void delete(Integer id) {
        if (!repository.existsById(id)) {
            throw new BusinessException("定价不存在");
        }
        repository.deleteById(id);
    }
}
