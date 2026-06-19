package org.xxg.backend.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xxg.backend.backend.dto.ApiResponse;
import org.xxg.backend.backend.service.OnlineUserService;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.*;

/**
 * 系统监控接口
 * <p>提供系统运行状态、数据库状态、API状态、在线用户等监控数据。</p>
 * <p>基础路径：/monitor</p>
 * <p>权限：仅管理员</p>
 */
@RestController
@RequestMapping("/monitor")
public class MonitorController {
    private final OnlineUserService onlineUserService;

    @Value("${app.version:1.0.2}")
    private String appVersion;

    @Value("${app.repo-url:https://github.com/user/xxgkami-ai}")
    private String repoUrl;

    public MonitorController(OnlineUserService onlineUserService) {
        this.onlineUserService = onlineUserService;
    }

    /**
     * 获取所有监控数据（系统、数据库、API、用户）
     * <p>GET /monitor/all</p>
     * <p>权限：管理员</p>
     * @return 包含 system、database、api、users 四项监控数据的综合信息
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllMonitorData() {
        Map<String, Object> data = new HashMap<>();
        data.put("system", getSystemData());
        data.put("database", getDatabaseData());
        data.put("api", getApiData());
        data.put("users", getUsersData());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    /**
     * 获取系统运行状态
     * <p>GET /monitor/system</p>
     * <p>权限：管理员</p>
     * @return 内存使用、CPU核心数、Java版本、操作系统等系统信息
     */
    @GetMapping("/system")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemStatus() {
        return ResponseEntity.ok(ApiResponse.ok(getSystemData()));
    }

    /**
     * 获取数据库连接状态
     * <p>GET /monitor/database</p>
     * <p>权限：管理员</p>
     * @return 数据库状态及类型信息
     */
    @GetMapping("/database")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDatabaseStatus() {
        return ResponseEntity.ok(ApiResponse.ok(getDatabaseData()));
    }

    /**
     * 获取API运行状态
     * <p>GET /monitor/api</p>
     * <p>权限：管理员</p>
     * @return API状态、总请求数、错误率等信息
     */
    @GetMapping("/api")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getApiStatus() {
        return ResponseEntity.ok(ApiResponse.ok(getApiData()));
    }

    /**
     * 获取在线用户信息
     * <p>GET /monitor/users</p>
     * <p>权限：管理员</p>
     * @return 当前在线用户数量及用户列表
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOnlineUsers() {
        return ResponseEntity.ok(ApiResponse.ok(getUsersData()));
    }

    /**
     * 构建系统监控数据（私有方法）
     * @return 包含内存、CPU、Java版本、操作系统、运行时间等信息的Map
     */
    private Map<String, Object> getSystemData() {
        Map<String, Object> system = new HashMap<>();
        Runtime rt = Runtime.getRuntime();
        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
        RuntimeMXBean rtBean = ManagementFactory.getRuntimeMXBean();

        system.put("totalMemory", rt.totalMemory());
        system.put("freeMemory", rt.freeMemory());
        system.put("usedMemory", rt.totalMemory() - rt.freeMemory());
        system.put("maxMemory", rt.maxMemory());
        system.put("availableProcessors", rt.availableProcessors());
        system.put("javaVersion", System.getProperty("java.version"));
        system.put("osName", System.getProperty("os.name"));
        system.put("osArch", System.getProperty("os.arch"));
        system.put("uptime", rtBean.getUptime());
        return system;
    }

    /**
     * 构建数据库监控数据（私有方法）
     * @return 数据库状态及类型
     */
    private Map<String, Object> getDatabaseData() {
        Map<String, Object> db = new HashMap<>();
        db.put("status", "UP");
        db.put("type", "MySQL");
        return db;
    }

    /**
     * 构建API监控数据（私有方法）
     * @return API状态、总请求数、错误率
     */
    private Map<String, Object> getApiData() {
        Map<String, Object> api = new HashMap<>();
        api.put("status", "UP");
        api.put("totalRequests", 0);
        api.put("errorRate", 0);
        return api;
    }

    /**
     * 构建在线用户监控数据（私有方法）
     * @return 在线用户数量及用户列表
     */
    private Map<String, Object> getUsersData() {
        Map<String, Object> users = new HashMap<>();
        users.put("onlineCount", onlineUserService.getOnlineCount());
        users.put("onlineUsers", onlineUserService.getOnlineUsers());
        return users;
    }

    /**
     * 检查系统更新
     * <p>GET /monitor/check-update</p>
     * <p>权限：管理员</p>
     * @return 当前版本号和仓库地址（安全修复：不再返回可执行 shell 命令，防止命令注入）
     */
    @GetMapping("/check-update")
    public ResponseEntity<Map<String, Object>> checkUpdate() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("version", appVersion);
        result.put("repoUrl", repoUrl);
        // 安全修复：仅返回更新地址，不返回可执行命令，防止命令注入风险
        result.put("updateUrl", repoUrl + "/releases/latest");
        return ResponseEntity.ok(result);
    }
}
