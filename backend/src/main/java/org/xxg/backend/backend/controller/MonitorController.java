package org.xxg.backend.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xxg.backend.backend.dto.ApiResponse;
import org.xxg.backend.backend.service.OnlineUserService;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.*;

@RestController
@RequestMapping("/monitor")
public class MonitorController {
    private final OnlineUserService onlineUserService;

    public MonitorController(OnlineUserService onlineUserService) {
        this.onlineUserService = onlineUserService;
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllMonitorData() {
        Map<String, Object> data = new HashMap<>();
        data.put("system", getSystemData());
        data.put("database", getDatabaseData());
        data.put("api", getApiData());
        data.put("users", getUsersData());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/system")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemStatus() {
        return ResponseEntity.ok(ApiResponse.ok(getSystemData()));
    }

    @GetMapping("/database")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDatabaseStatus() {
        return ResponseEntity.ok(ApiResponse.ok(getDatabaseData()));
    }

    @GetMapping("/api")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getApiStatus() {
        return ResponseEntity.ok(ApiResponse.ok(getApiData()));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOnlineUsers() {
        return ResponseEntity.ok(ApiResponse.ok(getUsersData()));
    }

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

    private Map<String, Object> getDatabaseData() {
        Map<String, Object> db = new HashMap<>();
        db.put("status", "UP");
        db.put("type", "MySQL");
        return db;
    }

    private Map<String, Object> getApiData() {
        Map<String, Object> api = new HashMap<>();
        api.put("status", "UP");
        api.put("totalRequests", 0);
        api.put("errorRate", 0);
        return api;
    }

    private Map<String, Object> getUsersData() {
        Map<String, Object> users = new HashMap<>();
        users.put("onlineCount", onlineUserService.getOnlineCount());
        users.put("onlineUsers", onlineUserService.getOnlineUsers());
        return users;
    }

    @GetMapping("/check-update")
    public ResponseEntity<Map<String, Object>> checkUpdate() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("version", "1.0.2");
        result.put("repoUrl", "https://github.com/user/xxgkami-ai");
        Map<String, String> scripts = new HashMap<>();
        scripts.put("cn", "curl -sSL https://raw.githubusercontent.com/user/xxgkami-ai/main/install.sh | bash");
        scripts.put("global", "curl -sSL https://raw.githubusercontent.com/user/xxgkami-ai/main/install.sh | bash");
        result.put("updateScripts", scripts);
        return ResponseEntity.ok(result);
    }
}
