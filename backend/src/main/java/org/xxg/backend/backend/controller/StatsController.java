package org.xxg.backend.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xxg.backend.backend.dto.ApiResponse;
import org.xxg.backend.backend.service.StatsService;
import java.util.Map;

@RestController
@RequestMapping("/stats")
public class StatsController {
    private final StatsService statsService;
    public StatsController(StatsService statsService) { this.statsService = statsService; }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dashboard() {
        return ResponseEntity.ok(ApiResponse.ok(statsService.getDashboardStats()));
    }

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> overview() {
        return ResponseEntity.ok(ApiResponse.ok(statsService.getOverview()));
    }

    @GetMapping("/user-activity")
    public ResponseEntity<ApiResponse<Map<String, Object>>> userActivity(@RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(ApiResponse.ok(statsService.getUserActivityStats(days)));
    }
}
