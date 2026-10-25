package org.xxg.backend.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xxg.backend.backend.dto.ApiResponse;
import org.xxg.backend.backend.dto.CardResponse;
import org.xxg.backend.backend.dto.GenerateCardRequest;
import org.xxg.backend.backend.dto.VerifyCardRequest;
import org.xxg.backend.backend.entity.Card;
import org.xxg.backend.backend.mapper.UserRepository;
import org.xxg.backend.backend.service.CardService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 卡密管理接口控制器
 * <p>提供卡密的生成、验证、查询、启停用、机器码解绑及统计等功能。</p>
 * <p>基础路径：/cards</p>
 * <p>包含前端兼容接口和标准REST接口两套端点。</p>
 */
@RestController
@RequestMapping("/cards")
// 必须标注 @Validated：方法参数上的 @Min/@Max（如 /trend 的 days）
// 仅对标注该注解的 Bean 生效，缺失时约束静默不校验
@org.springframework.validation.annotation.Validated
@Tag(name = "卡密管理", description = "卡密生成、验证、查询、启停用、机器码解绑")
public class CardController {

    /** 单次批量生成卡密的数量上限，防止超大请求耗尽内存 */
    private static final int MAX_BATCH_GENERATE_COUNT = 1000;

    private final CardService cardService;
    private final UserRepository userRepository;
    public CardController(CardService cardService, UserRepository userRepository) {
        this.cardService = cardService;
        this.userRepository = userRepository;
    }

    // --- Frontend-compatible endpoints ---

    @Operation(summary = "使用卡密", description = "公开接口，验证卡密有效性并绑定机器码。限流：30次/分钟/IP。")
    @PostMapping("/use")
    public ResponseEntity<Map<String, Object>> useCard(@RequestBody Map<String, String> body, HttpServletRequest request) {
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
        String machineCode = body.getOrDefault("device_id", body.get("machine_code"));
        // 安全修复：未提供机器码时拒绝验证，防止卡密被共享
        if (machineCode == null || machineCode.isBlank()) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("success", false);
            error.put("message", "机器码不能为空");
            error.put("statusCode", 400);
            return ResponseEntity.badRequest().body(error);
        }
        if (machineCode.length() > 255) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("success", false);
            error.put("message", "机器码长度不能超过255个字符");
            error.put("statusCode", 400);
            return ResponseEntity.badRequest().body(error);
        }
        return ResponseEntity.ok(cardService.verifyCard(cardKey, machineCode, null, getClientIp(request)));
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyCard(@Valid @RequestBody VerifyCardRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(cardService.verifyCard(request.getCardKey(), request.getMachineCode(),
                request.getApiKeyId(), getClientIp(httpRequest)));
    }

    @PostMapping("/admin/create")
    public ResponseEntity<ApiResponse<List<Card>>> adminCreate(@Valid @RequestBody GenerateCardRequest request) {
        return generateCard(request);
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<List<Card>>> generateCard(@Valid @RequestBody GenerateCardRequest request) {
        int count = request.getCount() != null ? request.getCount() : 1;
        // 安全修复：限制单次批量生成上限。count 来自请求体且无上限校验，
        // 超大值会一次性构建等量实体并触发 saveAll，导致内存耗尽 / 数据库长时间阻塞。
        if (count < 1 || count > MAX_BATCH_GENERATE_COUNT) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("单次生成数量必须在 1-" + MAX_BATCH_GENERATE_COUNT + " 之间"));
        }
        // 使用批量生成减少数据库往返，避免循环内逐个调用 generateCard。
        // resolveDays() 兼容只传 duration 的旧客户端，避免漏传 days 导致时长卡永不过期。
        List<Card> cards = cardService.generateCardsBatch(
                request.getCardType(), request.getDuration(), request.getTotalCount(),
                request.getCreatorType(), request.getCreatorId(), request.getCreatorName(),
                request.getVerifyMethod(), request.resolveDays(), request.getApiKeyId(), count,
                request.getAllowReverify(), request.getStackTimeIfSameMachine(), request.getAllowSelfUnbind());
        return ResponseEntity.ok(ApiResponse.ok("卡密生成成功，共 " + count + " 张", cards));
    }

    @GetMapping("/admin/all")
    public ResponseEntity<ApiResponse<Page<CardResponse>>> adminAllCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        // 安全修复：合并 /admin/all 和 /admin 功能，/admin/all 保留兼容但统一调用 getAdminCards 逻辑
        return getAdminCards(page, Math.min(size, 500));
    }

    @GetMapping("/admin")
    public ResponseEntity<ApiResponse<Page<CardResponse>>> getAdminCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        size = Math.min(size, 100); // 防止过大的分页请求导致 OOM
        Page<Card> cardPage = cardService.getCardsByCreator("admin", null, PageRequest.of(page, size));
        Page<CardResponse> responsePage = cardPage.map(CardResponse::fromEntity);
        return ResponseEntity.ok(ApiResponse.ok(responsePage));
    }

    @PutMapping("/admin/{id}")
    public ResponseEntity<ApiResponse<Card>> updateCard(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        Card updated = cardService.updateCard(id, body);
        return ResponseEntity.ok(ApiResponse.ok("卡密更新成功", updated));
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
            // 必须走 enableCard：setCardStatus 只改主表 status，不恢复
            // CardStatus.isValid——暂停后"启用"的卡密会永久无法通过验证
            // （verifyCard 校验 isValid=false 直接拒绝），而后台列表显示正常，
            // 管理员无法察觉。enableCard 会同步恢复 isValid 并按使用记录
            // 还原 0/1 状态，忽略传入的 status 值（由后端权威决定）
            cardService.enableCard(id);
        }
        return ResponseEntity.ok(ApiResponse.ok("状态已更新"));
    }

    @GetMapping("/apikey/{apiKeyId}")
    public ResponseEntity<ApiResponse<List<CardResponse>>> getApiKeyCards(@PathVariable Long apiKeyId) {
        List<Card> cards = cardService.getCardsByApiKeyId(apiKeyId.intValue());
        List<CardResponse> responses = cards.stream().map(CardResponse::fromEntity).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    @GetMapping("/trend")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUsageTrend(
            @RequestParam(defaultValue = "7") @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(365) int days) {
        return ResponseEntity.ok(ApiResponse.ok(cardService.getUsageTrend(days)));
    }

    // --- Standard endpoints ---

    /**
     * 获取当前登录用户自己的卡密列表。
     * <p>userId 从认证 Token 提取（非路径参数），普通用户与管理员均可访问，
     * 替代 ADMIN-only 的 /cards/user/{userId}（该接口保留给管理端跨用户查询）。</p>
     *
     * @param auth 当前认证信息
     * @return 当前用户的卡密列表（脱敏 DTO）
     */
    @GetMapping("/user/me")
    public ResponseEntity<ApiResponse<List<org.xxg.backend.backend.dto.UserCardResponse>>> getMyCards(
            org.springframework.security.core.Authentication auth) {
        if (auth == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("未登录"));
        }
        Integer userId = userRepository.findByUsername(auth.getName())
                .map(org.xxg.backend.backend.entity.User::getId)
                .orElseThrow(() -> new org.xxg.backend.backend.exception.BusinessException("当前账号未关联用户"));
        // 归属查询需同时覆盖两类来源：后台分配（creatorType=user）
        // 与订单支付生成（creatorType=system，creatorId=购买者）。
        // 此前仅按 creatorType=user 过滤，订单购买的卡不出现在"我的卡密"
        //
        // 使用 UserCardResponse 而非 CardResponse：后者返回 encryptedKey/deviceId/
        // creatorId 等内部字段，其类注释明确"普通用户接口不得使用此 DTO"
        List<Card> cards = cardService.getOwnedCards(userId);
        List<org.xxg.backend.backend.dto.UserCardResponse> responses = cards.stream()
                .map(org.xxg.backend.backend.dto.UserCardResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<CardResponse>>> getUserCards(@PathVariable Integer userId) {
        List<Card> cards = cardService.getCardsByCreator("user", userId);
        List<CardResponse> responses = cards.stream().map(CardResponse::fromEntity).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(responses));
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

    /**
     * 提取客户端 IP：优先取 X-Forwarded-For 首个值（反向代理场景），否则取直连地址。
     * 仅用于审计记录——XFF 可被客户端伪造，不得作为限流/封禁等安全决策的唯一依据。
     */
    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
