package org.xxg.backend.backend.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xxg.backend.backend.dto.ApiResponse;
import org.xxg.backend.backend.dto.GenerateCardRequest;
import org.xxg.backend.backend.dto.VerifyCardRequest;
import org.xxg.backend.backend.entity.Card;
import org.xxg.backend.backend.service.CardService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cards")
public class CardController {
    private final CardService cardService;
    public CardController(CardService cardService) { this.cardService = cardService; }

    // --- Frontend-compatible endpoints ---

    @PostMapping("/use")
    public ResponseEntity<Map<String, Object>> useCard(@RequestBody Map<String, String> body) {
        String cardKey = body.get("card_key");
        if (cardKey == null || cardKey.isBlank()) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("success", false);
            error.put("message", "卡密不能为空");
            error.put("statusCode", 400);
            return ResponseEntity.badRequest().body(error);
        }
        // client sends "device_id", mapped to machineCode for binding
        String machineCode = body.getOrDefault("device_id", body.getOrDefault("machine_code", "Unknown"));
        return ResponseEntity.ok(cardService.verifyCard(cardKey, machineCode, null));
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyCard(@Valid @RequestBody VerifyCardRequest request) {
        return ResponseEntity.ok(cardService.verifyCard(request.getCardKey(), request.getMachineCode(), request.getApiKeyId()));
    }

    @PostMapping("/admin/create")
    public ResponseEntity<ApiResponse<Card>> adminCreate(@Valid @RequestBody GenerateCardRequest request) throws Exception {
        return generateCard(request);
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<Card>> generateCard(@Valid @RequestBody GenerateCardRequest request) throws Exception {
        Card card = cardService.generateCard(request.getCardType(), request.getDuration(), request.getTotalCount(),
                request.getCreatorType(), request.getCreatorId(), request.getCreatorName(),
                request.getVerifyMethod(), request.getDays(), request.getApiKeyId());
        return ResponseEntity.ok(ApiResponse.ok("卡密生成成功", card));
    }

    @GetMapping("/admin/all")
    public ResponseEntity<ApiResponse<List<Card>>> adminAllCards() {
        return ResponseEntity.ok(ApiResponse.ok(cardService.getCardsByCreator("admin", null)));
    }

    @GetMapping("/admin")
    public ResponseEntity<ApiResponse<Page<Card>>> getAdminCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                cardService.getCardsByCreator("admin", null, PageRequest.of(page, size))));
    }

    @PutMapping("/admin/{id}")
    public ResponseEntity<ApiResponse<Card>> updateCard(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        return ResponseEntity.status(501).body(ApiResponse.error("卡密更新功能暂未实现"));
    }

    @PatchMapping("/admin/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateCardStatus(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        Number statusNum = (Number) body.get("status");
        Integer status = statusNum != null ? statusNum.intValue() : null;
        if (status != null && status == 2) {
            cardService.disableCard(id);
        } else {
            cardService.enableCard(id);
        }
        return ResponseEntity.ok(ApiResponse.ok("状态已更新"));
    }

    @GetMapping("/apikey/{apiKeyId}")
    public ResponseEntity<ApiResponse<List<Card>>> getApiKeyCards(@PathVariable Long apiKeyId) {
        return ResponseEntity.ok(ApiResponse.ok(List.of()));
    }

    @GetMapping("/trend")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUsageTrend(@RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("trend", List.of())));
    }

    // --- Standard endpoints ---

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<Card>>> getUserCards(@PathVariable Integer userId) {
        return ResponseEntity.ok(ApiResponse.ok(cardService.getCardsByCreator("user", userId)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCard(@PathVariable Integer id) {
        cardService.deleteCard(id);
        return ResponseEntity.ok(ApiResponse.ok("卡密已删除"));
    }

    @PutMapping("/{id}/disable")
    public ResponseEntity<ApiResponse<Void>> disableCard(@PathVariable Integer id) {
        cardService.disableCard(id);
        return ResponseEntity.ok(ApiResponse.ok("卡密已停用"));
    }

    @PutMapping("/{id}/enable")
    public ResponseEntity<ApiResponse<Void>> enableCard(@PathVariable Integer id) {
        cardService.enableCard(id);
        return ResponseEntity.ok(ApiResponse.ok("卡密已启用"));
    }

    @PutMapping("/{id}/unbind")
    public ResponseEntity<ApiResponse<Void>> unbindMachineCode(@PathVariable Integer id) {
        cardService.unbindMachineCode(id);
        return ResponseEntity.ok(ApiResponse.ok("机器码已解绑"));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok(cardService.getStats()));
    }
}
