package org.xxg.backend.backend.service;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

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

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("users", userService.getUserStats());
        stats.put("cards", cardService.getStats());
        stats.put("orders", orderService.getOrderStats());
        stats.put("timestamp", System.currentTimeMillis());
        return stats;
    }

    public Map<String, Object> getOverview() {
        Map<String, Object> overview = new HashMap<>();
        overview.put("todayStart", LocalDateTime.now().toLocalDate().atStartOfDay().toString());
        overview.put("dashboard", getDashboardStats());
        return overview;
    }

    public Map<String, Object> getUserActivityStats(int days) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("period", days);
        stats.put("activeUsers", userService.getUserStats().getOrDefault("totalUsers", 0));
        stats.put("newRegistrations", 0);
        stats.put("loginCount", 0);
        return stats;
    }
}
