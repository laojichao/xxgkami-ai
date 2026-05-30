package org.xxg.backend.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xxg.backend.backend.dto.ApiResponse;
import org.xxg.backend.backend.entity.CardPricing;
import org.xxg.backend.backend.service.CardPricingService;
import java.util.List;

@RestController
public class CardPricingController {
    private final CardPricingService service;
    public CardPricingController(CardPricingService service) { this.service = service; }

    @GetMapping({"/card-pricing", "/pricing"})
    public ResponseEntity<ApiResponse<List<CardPricing>>> list(@RequestParam(required = false) String type) {
        return ResponseEntity.ok(ApiResponse.ok(type != null ? service.getByType(type) : service.getAll()));
    }

    @PostMapping({"/card-pricing", "/pricing"})
    public ResponseEntity<ApiResponse<CardPricing>> save(@RequestBody CardPricing pricing) {
        return ResponseEntity.ok(ApiResponse.ok(service.save(pricing)));
    }

    @PutMapping({"/card-pricing/{id}", "/pricing/{id}"})
    public ResponseEntity<ApiResponse<CardPricing>> update(@PathVariable Integer id, @RequestBody CardPricing pricing) {
        pricing.setId(id);
        return ResponseEntity.ok(ApiResponse.ok(service.save(pricing)));
    }

    @DeleteMapping({"/card-pricing/{id}", "/pricing/{id}"})
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("已删除"));
    }
}
