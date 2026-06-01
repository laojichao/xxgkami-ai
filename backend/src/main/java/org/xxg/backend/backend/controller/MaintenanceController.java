package org.xxg.backend.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xxg.backend.backend.dto.ApiResponse;
import org.xxg.backend.backend.entity.MaintenanceSettings;
import org.xxg.backend.backend.service.MaintenanceService;
import org.xxg.backend.backend.service.BackupService;

import java.util.Map;

/**
 * 系统维护接口
 * <p>提供系统维护模式的配置、状态查询，以及缓存/日志清理功能。</p>
 * <p>基础路径：/maintenance</p>
 * <p>权限：仅管理员</p>
 */
@RestController
@RequestMapping("/maintenance")
public class MaintenanceController {
    private final MaintenanceService maintenanceService;
    private final BackupService backupService;

    public MaintenanceController(MaintenanceService maintenanceService, BackupService backupService) {
        this.maintenanceService = maintenanceService;
        this.backupService = backupService;
    }

    /**
     * 获取当前维护模式配置
     * <p>GET /maintenance</p>
     * <p>权限：管理员</p>
     * @return 维护模式的配置信息
     */
    @GetMapping
    public ResponseEntity<ApiResponse<MaintenanceSettings>> get() {
        return ResponseEntity.ok(ApiResponse.ok(maintenanceService.getSettings()));
    }

    /**
     * 获取维护模式状态概览
     * <p>GET /maintenance/status</p>
     * <p>权限：管理员</p>
     * @return 包含 enabled（是否开启维护模式）和 settings（配置详情）的状态信息
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> status() {
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("enabled", maintenanceService.isMaintenanceMode());
        result.put("settings", maintenanceService.getSettings());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 更新维护模式配置（PUT方式）
     * <p>PUT /maintenance</p>
     * <p>权限：管理员</p>
     * @param settings 新的维护配置
     * @return 更新后的配置信息
     */
    @PutMapping
    public ResponseEntity<ApiResponse<MaintenanceSettings>> update(@RequestBody MaintenanceSettings settings) {
        return ResponseEntity.ok(ApiResponse.ok(maintenanceService.updateSettings(settings)));
    }

    /**
     * 更新维护模式配置（POST方式）
     * <p>POST /maintenance/update</p>
     * <p>权限：管理员</p>
     * @param settings 新的维护配置
     * @return 更新后的配置信息
     */
    @PostMapping("/update")
    public ResponseEntity<ApiResponse<MaintenanceSettings>> updateSettings(@RequestBody MaintenanceSettings settings) {
        return ResponseEntity.ok(ApiResponse.ok(maintenanceService.updateSettings(settings)));
    }

    /**
     * 清理系统缓存（暂未实现）
     * <p>POST /maintenance/clear-cache</p>
     * <p>权限：管理员</p>
     * @return 501 未实现
     */
    @PostMapping("/clear-cache")
    public ResponseEntity<ApiResponse<Void>> clearCache() {
        return ResponseEntity.status(501).body(ApiResponse.error("缓存清理功能暂未实现"));
    }

    /**
     * 清理系统日志（暂未实现）
     * <p>POST /maintenance/clear-logs</p>
     * <p>权限：管理员</p>
     * @return 501 未实现
     */
    @PostMapping("/clear-logs")
    public ResponseEntity<ApiResponse<Void>> clearLogs() {
        return ResponseEntity.status(501).body(ApiResponse.error("日志清理功能暂未实现"));
    }
}

/**
 * 数据备份接口（别名控制器）
 * <p>提供数据库备份功能，基础路径：/backup</p>
 * <p>权限：仅管理员</p>
 */
@RestController
@RequestMapping("/backup")
class BackupControllerAlias {
    private final BackupService backupService;
    BackupControllerAlias(BackupService backupService) { this.backupService = backupService; }

    /**
     * 执行数据库备份
     * <p>POST /backup</p>
     * <p>权限：管理员</p>
     * @return 备份文件路径
     */
    @PostMapping
    public ResponseEntity<ApiResponse<String>> backup() throws Exception {
        return ResponseEntity.ok(ApiResponse.ok("备份成功", backupService.backup()));
    }

    /**
     * 创建数据库备份（别名接口）
     * <p>POST /backup/create</p>
     * <p>权限：管理员</p>
     * @return 备份文件路径
     */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<String>> createBackup() throws Exception {
        return ResponseEntity.ok(ApiResponse.ok("备份成功", backupService.backup()));
    }
}
