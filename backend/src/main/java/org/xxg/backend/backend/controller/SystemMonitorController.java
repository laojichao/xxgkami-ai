package org.xxg.backend.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xxg.backend.backend.dto.ApiResponse;
import java.lang.management.*;
import java.util.*;

@RestController
@RequestMapping("/system")
public class SystemMonitorController {

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

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "timestamp", String.valueOf(System.currentTimeMillis())));
    }
}
