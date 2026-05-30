package org.xxg.backend.backend.service;

import org.springframework.stereotype.Service;
import org.xxg.backend.backend.entity.CardPricing;
import org.xxg.backend.backend.exception.BusinessException;
import org.xxg.backend.backend.mapper.CardPricingRepository;
import java.util.List;

@Service
public class CardPricingService {
    private final CardPricingRepository repository;
    public CardPricingService(CardPricingRepository repository) { this.repository = repository; }

    public List<CardPricing> getAll() { return repository.findAll(); }
    public List<CardPricing> getByType(String type) { return repository.findByType(type); }
    public CardPricing getById(Integer id) {
        return repository.findById(id).orElseThrow(() -> new BusinessException("定价不存在"));
    }
    public CardPricing save(CardPricing pricing) { return repository.save(pricing); }
    public void delete(Integer id) { repository.deleteById(id); }
}
