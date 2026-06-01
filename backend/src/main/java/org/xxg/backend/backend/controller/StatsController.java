package org.xxg.backend.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xxg.backend.backend.dto.ApiResponse;
import org.xxg.backend.backend.service.StatsService;
import java.util.Map;

/**
 * 统计数据接口
 * <p>提供仪表盘统计、数据概览及用户活跃度统计功能。</p>
 * <p>基础路径：/stats</p>
 * <p>权限：仅管理员</p>
 */
@RestController
@RequestMapping("/stats")
public class StatsController {
    private final StatsService statsService;
    public StatsController(StatsService statsService) { this.statsService = statsService; }

    /**
     * 获取仪表盘统计数据
     * <p>GET /stats/dashboard</p>
     * <p>权限：管理员</p>
     * <p>返回管理员后台首页所需的综合统计数据。</p>
     * @return 仪表盘统计信息（如订单数、用户数、收入等）
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dashboard() {
        return ResponseEntity.ok(ApiResponse.ok(statsService.getDashboardStats()));
    }

    /**
     * 获取系统数据概览
     * <p>GET /stats/overview</p>
     * <p>权限：管理员</p>
     * @return 系统整体数据概览信息
     */
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> overview() {
        return ResponseEntity.ok(ApiResponse.ok(statsService.getOverview()));
    }

    /**
     * 获取用户活跃度统计
     * <p>GET /stats/user-activity</p>
     * <p>权限：管理员</p>
     * @param days 统计天数，默认7天
     * @return 指定天数范围内的用户活跃度统计数据
     */
    @GetMapping("/user-activity")
    public ResponseEntity<ApiResponse<Map<String, Object>>> userActivity(@RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(ApiResponse.ok(statsService.getUserActivityStats(days)));
    }
}
