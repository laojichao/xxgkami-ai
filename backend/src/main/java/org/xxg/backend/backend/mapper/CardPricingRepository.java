package org.xxg.backend.backend.mapper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.xxg.backend.backend.entity.CardPricing;
import java.util.List;
import java.util.Optional;

public interface CardPricingRepository extends JpaRepository<CardPricing, Integer> {
    List<CardPricing> findByType(String type);
    Optional<CardPricing> findByTypeAndValue(String type, Integer value);
}
