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

/**
 * 卡密管理接口控制器
 * <p>提供卡密的生成、验证、查询、启停用、机器码解绑及统计等功能。</p>
 * <p>基础路径：/cards</p>
 * <p>包含前端兼容接口和标准REST接口两套端点。</p>
 */
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
        if (cardKey.length() > 255) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("success", false);
            error.put("message", "卡密长度不能超过255个字符");
            error.put("statusCode", 400);
            return ResponseEntity.badRequest().body(error);
        }
        // client sends "device_id", mapped to machineCode for binding
        String machineCode = body.getOrDefault("device_id", body.getOrDefault("machine_code", "Unknown"));
        if (machineCode != null && machineCode.length() > 255) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("success", false);
            error.put("message", "机器码长度不能超过255个字符");
            error.put("statusCode", 400);
            return ResponseEntity.badRequest().body(error);
        }
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
    public ResponseEntity<ApiResponse<Page<Card>>> adminAllCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        size = Math.min(size, 500); // Cap result size to prevent OOM
        return ResponseEntity.ok(ApiResponse.ok(
                cardService.getCardsByCreator("admin", null, PageRequest.of(page, size))));
    }

    @GetMapping("/admin")
    public ResponseEntity<ApiResponse<Page<Card>>> getAdminCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        size = Math.min(size, 100); // 防止过大的分页请求导致 OOM
        return ResponseEntity.ok(ApiResponse.ok(
                cardService.getCardsByCreator("admin", null, PageRequest.of(page, size))));
    }

    @PutMapping("/admin/{id}")
    public ResponseEntity<ApiResponse<Card>> updateCard(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        return ResponseEntity.status(501).body(ApiResponse.error("卡密更新功能暂未实现"));
    }

    @PatchMapping("/admin/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateCardStatus(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        Object statusObj = body.get("status");
        Integer status = null;
        if (statusObj instanceof Number) {
            status = ((Number) statusObj).intValue();
        } else if (statusObj instanceof String) {
            try {
                status = Integer.parseInt((String) statusObj);
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body(ApiResponse.error("无效的状态值"));
            }
        }
        if (status == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("状态值不能为空"));
        }
        // 只接受合法的状态值：0(未使用)、1(已使用)、2(已停用)
        if (status != 0 && status != 1 && status != 2) {
            return ResponseEntity.badRequest().body(ApiResponse.error("无效的状态值，仅支持 0(未使用)、1(已使用)、2(已停用)"));
        }
        if (status == 2) {
            cardService.disableCard(id);
        } else {
            cardService.setCardStatus(id, status);
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

    @GetMapping("/query")
    public ResponseEntity<ApiResponse<Map<String, Object>>> queryCard(@RequestParam String cardKey) {
        Card card = cardService.getCardByKey(cardKey);
        if (card == null) {
            return ResponseEntity.ok(ApiResponse.error("卡密不存在"));
        }
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("cardKey", card.getCardKey());
        result.put("status", card.getStatus());
        result.put("cardType", card.getCardType() != null ? card.getCardType().name() : null);
        result.put("createTime", card.getCreateTime());
        result.put("useTime", card.getUseTime());
        result.put("expireTime", card.getExpireTime());
        result.put("duration", card.getDuration());
        result.put("totalCount", card.getTotalCount());
        result.put("remainingCount", card.getRemainingCount());
        // 机器码脱敏：仅返回是否已绑定，不暴露完整机器码
        String mc = card.getMachineCode();
        result.put("machineCodeBound", mc != null && !mc.isEmpty());
        result.put("verifyMethod", card.getVerifyMethod() != null ? card.getVerifyMethod().name() : null);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
