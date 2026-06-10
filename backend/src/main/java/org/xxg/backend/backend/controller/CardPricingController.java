package org.xxg.backend.backend.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xxg.backend.backend.dto.ApiResponse;
import org.xxg.backend.backend.entity.CardPricing;
import org.xxg.backend.backend.service.CardPricingService;
import java.util.List;

/**
 * 卡密定价管理接口
 * <p>提供卡密定价方案的增删改查功能，支持按类型筛选。</p>
 * <p>基础路径：/card-pricing 或 /pricing</p>
 * <p>权限：查询接口公开访问，增删改接口仅管理员</p>
 */
@RestController
public class CardPricingController {
    private final CardPricingService service;
    public CardPricingController(CardPricingService service) { this.service = service; }

    /**
     * 获取卡密定价列表
     * <p>GET /card-pricing 或 /pricing</p>
     * <p>权限：公开访问</p>
     * @param type 可选参数，按卡密类型筛选
     * @return 定价列表，若指定type则返回该类型下的定价
     */
    @GetMapping({"/card-pricing", "/pricing"})
    public ResponseEntity<ApiResponse<List<CardPricing>>> list(@RequestParam(required = false) String type) {
        return ResponseEntity.ok(ApiResponse.ok(type != null ? service.getByType(type) : service.getAll()));
    }

    /**
     * 新增卡密定价方案
     * <p>POST /card-pricing 或 /pricing</p>
     * <p>权限：管理员</p>
     * @param pricing 定价实体，包含类型、价格、有效期等信息
     * @return 新创建的定价信息
     */
    @PostMapping({"/card-pricing", "/pricing"})
    public ResponseEntity<ApiResponse<CardPricing>> save(@Valid @RequestBody CardPricing pricing) {
        return ResponseEntity.ok(ApiResponse.ok(service.save(pricing)));
    }

    /**
     * 更新指定定价方案
     * <p>PUT /card-pricing/{id} 或 /pricing/{id}</p>
     * <p>权限：管理员</p>
     * @param id 定价方案的ID
     * @param pricing 更新后的定价信息
     * @return 更新后的定价信息
     */
    @PutMapping({"/card-pricing/{id}", "/pricing/{id}"})
    public ResponseEntity<ApiResponse<CardPricing>> update(@PathVariable Integer id, @Valid @RequestBody CardPricing pricing) {
        pricing.setId(id);
        return ResponseEntity.ok(ApiResponse.ok(service.save(pricing)));
    }

    /**
     * 删除指定定价方案
     * <p>DELETE /card-pricing/{id} 或 /pricing/{id}</p>
     * <p>权限：管理员</p>
     * @param id 定价方案的ID
     * @return 操作结果
     */
    @DeleteMapping({"/card-pricing/{id}", "/pricing/{id}"})
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("已删除"));
    }
}
