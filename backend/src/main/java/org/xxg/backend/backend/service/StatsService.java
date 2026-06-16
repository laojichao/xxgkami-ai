package org.xxg.backend.backend.service;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 统计数据服务
 * 汇总用户、卡密、订单等维度的统计数据，提供仪表盘和概览数据
 */
@Service
public class StatsService {
    private final UserService userService;
    private final CardService cardService;
    private final OrderService orderService;

    public StatsService(UserService userService, CardService cardService, OrderService orderService) {
        this.userService = userService;
        this.cardService = cardService;
        this.orderService = orderService;
    }

    /**
     * 获取仪表盘统计数据
     * @return 包含用户、卡密、订单统计及时间戳的Map
     */
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("users", userService.getUserStats());
        stats.put("cards", cardService.getStats());
        stats.put("orders", orderService.getOrderStats());
        stats.put("timestamp", System.currentTimeMillis());
        return stats;
    }

    /**
     * 获取系统概览数据
     * @return 包含今日起始时间和仪表盘数据的Map
     */
    public Map<String, Object> getOverview() {
        Map<String, Object> overview = new HashMap<>();
        overview.put("todayStart", LocalDateTime.now().toLocalDate().atStartOfDay().toString());
        overview.put("dashboard", getDashboardStats());
        return overview;
    }

    /**
     * 获取指定天数内的用户活跃度统计
     * @param days 统计天数
     * @return 包含活跃用户数、新注册数、登录次数的Map
     */
    public Map<String, Object> getUserActivityStats(int days) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("period", days);
        // 活跃用户数：暂用总用户数近似（需接入登录日志统计真实活跃数）
        stats.put("activeUsers", userService.getUserStats().getOrDefault("totalUsers", 0));
        // 新注册用户数：统计指定天数内注册的用户
        stats.put("newRegistrations", userService.getNewUserCount(days));
        // 登录次数：需要登录日志表支持，暂返回 0
        stats.put("loginCount", 0);
        return stats;
    }
}
