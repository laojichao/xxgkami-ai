package org.xxg.backend.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xxg.backend.backend.dto.ApiResponse;
import org.xxg.backend.backend.entity.Feature;
import org.xxg.backend.backend.entity.Slide;
import org.xxg.backend.backend.mapper.FeatureRepository;
import org.xxg.backend.backend.mapper.SlideRepository;
import org.xxg.backend.backend.entity.Card;
import org.xxg.backend.backend.mapper.CardRepository;
import org.xxg.backend.backend.service.CardService;

import java.util.*;

/**
 * 公开接口控制器。
 * <p>提供无需登录即可访问的公开 API，包括功能特性列表、轮播图查询、
 * 以及卡密机器码的自助查询与解绑。</p>
 * <p>基础路径：{@code /public}</p>
 */
@RestController
@RequestMapping("/public")
public class PublicController {
    private final FeatureRepository featureRepository;
    private final SlideRepository slideRepository;
    private final CardRepository cardRepository;
    private final CardService cardService;

    public PublicController(FeatureRepository featureRepository, SlideRepository slideRepository,
                            CardRepository cardRepository, CardService cardService) {
        this.featureRepository = featureRepository;
        this.slideRepository = slideRepository;
        this.cardRepository = cardRepository;
        this.cardService = cardService;
    }

    /**
     * 获取已启用的功能特性列表，按排序顺序返回。
     *
     * @return 功能特性列表
     */
    @GetMapping("/features")
    public ResponseEntity<ApiResponse<List<Feature>>> getFeatures() {
        return ResponseEntity.ok(ApiResponse.ok(featureRepository.findByStatusTrueOrderBySortOrderAsc()));
    }

    /**
     * 获取已启用的轮播图列表，按排序顺序返回。
     *
     * @return 轮播图列表
     */
    @GetMapping("/slides")
    public ResponseEntity<ApiResponse<List<Slide>>> getSlides() {
        return ResponseEntity.ok(ApiResponse.ok(slideRepository.findByStatusTrueOrderBySortOrderAsc()));
    }

    /**
     * 查询卡密的机器码绑定状态。
     *
     * @param body 请求体，包含 card_key 字段
     * @return 包含 bound（是否已绑定）的结果
     */
    @PostMapping("/cards/machine-bind/query")
    public ResponseEntity<ApiResponse<Map<String, Object>>> machineBindQuery(@RequestBody Map<String, String> body) {
        String cardKey = body.get("card_key");
        if (cardKey == null || cardKey.isBlank()) {
            return ResponseEntity.ok(ApiResponse.error("卡密不能为空"));
        }
        if (cardKey.length() > 255) {
            return ResponseEntity.ok(ApiResponse.error("卡密格式不正确"));
        }
        Card card = cardRepository.findByCardKey(cardKey).orElse(null);
        Map<String, Object> result = new HashMap<>();
        // 统一错误消息，防止通过不同响应枚举有效卡密
        if (card == null || card.getStatus() == 2) {
            return ResponseEntity.ok(ApiResponse.error("卡密无效或已停用"));
        }
        result.put("bound", card.getMachineCode() != null && !card.getMachineCode().isEmpty());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 自助解绑卡密的机器码。
     * <p>需要卡密允许自助解绑，且必须提供当前绑定的机器码进行验证。</p>
     *
     * @param body 请求体，包含 card_key 和 machine_code 字段
     * @return 操作结果
     */
    @PostMapping("/cards/machine-bind/unbind")
    public ResponseEntity<ApiResponse<Void>> machineUnbind(@RequestBody Map<String, String> body) {
        String cardKey = body.get("card_key");
        if (cardKey == null || cardKey.isBlank()) {
            return ResponseEntity.ok(ApiResponse.error("卡密不能为空"));
        }
        if (cardKey.length() > 255) {
            return ResponseEntity.ok(ApiResponse.error("卡密格式不正确"));
        }
        String machineCode = body.get("machine_code");
        if (machineCode == null || machineCode.isBlank()) {
            return ResponseEntity.ok(ApiResponse.error("请提供当前机器码"));
        }
        if (machineCode.length() > 255) {
            return ResponseEntity.ok(ApiResponse.error("机器码格式不正确"));
        }
        cardService.selfUnbindMachineCode(cardKey, machineCode);
        return ResponseEntity.ok(ApiResponse.ok("解绑成功"));
    }
}
