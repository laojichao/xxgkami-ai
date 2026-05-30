package org.xxg.backend.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xxg.backend.backend.dto.ApiResponse;
import org.xxg.backend.backend.entity.MaintenanceSettings;
import org.xxg.backend.backend.service.MaintenanceService;
import org.xxg.backend.backend.service.BackupService;

import java.util.Map;

@RestController
@RequestMapping("/maintenance")
public class MaintenanceController {
    private final MaintenanceService maintenanceService;
    private final BackupService backupService;

    public MaintenanceController(MaintenanceService maintenanceService, BackupService backupService) {
        this.maintenanceService = maintenanceService;
        this.backupService = backupService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<MaintenanceSettings>> get() {
        return ResponseEntity.ok(ApiResponse.ok(maintenanceService.getSettings()));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> status() {
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("enabled", maintenanceService.isMaintenanceMode());
        result.put("settings", maintenanceService.getSettings());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<MaintenanceSettings>> update(@RequestBody MaintenanceSettings settings) {
        return ResponseEntity.ok(ApiResponse.ok(maintenanceService.updateSettings(settings)));
    }

    @PostMapping("/update")
    public ResponseEntity<ApiResponse<MaintenanceSettings>> updateSettings(@RequestBody MaintenanceSettings settings) {
        return ResponseEntity.ok(ApiResponse.ok(maintenanceService.updateSettings(settings)));
    }

    @PostMapping("/clear-cache")
    public ResponseEntity<ApiResponse<Void>> clearCache() {
        return ResponseEntity.status(501).body(ApiResponse.error("缓存清理功能暂未实现"));
    }

    @PostMapping("/clear-logs")
    public ResponseEntity<ApiResponse<Void>> clearLogs() {
        return ResponseEntity.status(501).body(ApiResponse.error("日志清理功能暂未实现"));
    }
}

@RestController
@RequestMapping("/backup")
class BackupControllerAlias {
    private final BackupService backupService;
    BackupControllerAlias(BackupService backupService) { this.backupService = backupService; }

    @PostMapping
    public ResponseEntity<ApiResponse<String>> backup() throws Exception {
        return ResponseEntity.ok(ApiResponse.ok("备份成功", backupService.backup()));
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<String>> createBackup() throws Exception {
        return ResponseEntity.ok(ApiResponse.ok("备份成功", backupService.backup()));
    }
}
