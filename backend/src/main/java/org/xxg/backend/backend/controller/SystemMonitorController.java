package org.xxg.backend.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xxg.backend.backend.dto.ApiResponse;
import java.lang.management.*;
import java.util.*;

/**
 * 系统信息接口
 * <p>提供服务器运行环境信息查询和健康检查功能。</p>
 * <p>基础路径：/system</p>
 */
@RestController
@RequestMapping("/system")
public class SystemMonitorController {

    /**
     * 获取系统运行环境信息
     * <p>GET /system/info</p>
     * <p>权限：管理员</p>
     * <p>返回JVM内存使用情况、CPU核心数、Java版本、操作系统等服务器环境信息。</p>
     * @return 包含 totalMemory、freeMemory、usedMemory、maxMemory、availableProcessors、javaVersion、osName、osArch 等字段的系统信息
     */
    @GetMapping("/info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> systemInfo() {
        Map<String, Object> info = new HashMap<>();
        Runtime runtime = Runtime.getRuntime();
        info.put("totalMemory", runtime.totalMemory());
        info.put("freeMemory", runtime.freeMemory());
        info.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
        info.put("maxMemory", runtime.maxMemory());
        info.put("availableProcessors", runtime.availableProcessors());
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("osName", System.getProperty("os.name"));
        info.put("osArch", System.getProperty("os.arch"));
        return ResponseEntity.ok(ApiResponse.ok(info));
    }

    /**
     * 系统健康检查
     * <p>GET /system/health</p>
     * <p>权限：公开访问</p>
     * <p>用于负载均衡器或监控系统检测服务是否正常运行。</p>
     * @return 包含 status（"UP"）和 timestamp 的健康状态信息
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "timestamp", String.valueOf(System.currentTimeMillis())));
    }
}
