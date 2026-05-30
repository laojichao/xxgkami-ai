package org.xxg.backend.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xxg.backend.backend.dto.ApiResponse;
import org.xxg.backend.backend.entity.Feature;
import org.xxg.backend.backend.entity.Slide;
import org.xxg.backend.backend.mapper.FeatureRepository;
import org.xxg.backend.backend.mapper.SlideRepository;
import org.xxg.backend.backend.service.CardService;
import org.xxg.backend.backend.entity.Card;
import org.xxg.backend.backend.mapper.CardRepository;

import java.util.*;

@RestController
@RequestMapping("/public")
public class PublicController {
    private final FeatureRepository featureRepository;
    private final SlideRepository slideRepository;
    private final CardRepository cardRepository;

    public PublicController(FeatureRepository featureRepository, SlideRepository slideRepository,
                            CardRepository cardRepository) {
        this.featureRepository = featureRepository;
        this.slideRepository = slideRepository;
        this.cardRepository = cardRepository;
    }

    @GetMapping("/features")
    public ResponseEntity<ApiResponse<List<Feature>>> getFeatures() {
        return ResponseEntity.ok(ApiResponse.ok(featureRepository.findByStatusTrueOrderBySortOrderAsc()));
    }

    @GetMapping("/slides")
    public ResponseEntity<ApiResponse<List<Slide>>> getSlides() {
        return ResponseEntity.ok(ApiResponse.ok(slideRepository.findByStatusTrueOrderBySortOrderAsc()));
    }

    @PostMapping("/cards/machine-bind/query")
    public ResponseEntity<ApiResponse<Map<String, Object>>> machineBindQuery(@RequestBody Map<String, String> body) {
        String cardKey = body.get("card_key");
        if (cardKey == null || cardKey.isBlank()) {
            return ResponseEntity.ok(ApiResponse.error("卡密不能为空"));
        }
        Card card = cardRepository.findByCardKey(cardKey).orElse(null);
        Map<String, Object> result = new HashMap<>();
        if (card == null) {
            return ResponseEntity.ok(ApiResponse.error("卡密不存在"));
        }
        result.put("bound", card.getMachineCode() != null && !card.getMachineCode().isEmpty());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/cards/machine-bind/unbind")
    public ResponseEntity<ApiResponse<Void>> machineUnbind(@RequestBody Map<String, String> body) {
        String cardKey = body.get("card_key");
        if (cardKey == null || cardKey.isBlank()) {
            return ResponseEntity.ok(ApiResponse.error("卡密不能为空"));
        }
        Card card = cardRepository.findByCardKey(cardKey).orElse(null);
        if (card == null) {
            return ResponseEntity.ok(ApiResponse.error("卡密不存在"));
        }
        if (!card.getAllowSelfUnbind()) {
            return ResponseEntity.ok(ApiResponse.error("此卡密不允许自助解绑"));
        }
        // Verify machine code matches before unbinding
        String machineCode = body.get("machine_code");
        if (machineCode == null || machineCode.isBlank()) {
            return ResponseEntity.ok(ApiResponse.error("请提供当前机器码"));
        }
        if (card.getMachineCode() == null || !card.getMachineCode().equals(machineCode)) {
            return ResponseEntity.ok(ApiResponse.error("机器码不匹配"));
        }
        card.setMachineCode(null);
        cardRepository.save(card);
        return ResponseEntity.ok(ApiResponse.ok("解绑成功"));
    }
}
