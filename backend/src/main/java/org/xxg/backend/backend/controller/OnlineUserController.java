package org.xxg.backend.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xxg.backend.backend.dto.ApiResponse;
import org.xxg.backend.backend.service.OnlineUserService;
import java.util.*;

@RestController
public class OnlineUserController {
    private final OnlineUserService service;
    public OnlineUserController(OnlineUserService service) { this.service = service; }

    @GetMapping({"/online-users", "/online/list"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOnlineUsers() {
        Map<String, Object> result = new HashMap<>();
        result.put("count", service.getOnlineCount());
        result.put("users", service.getOnlineUsers());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/online/login")
    public ResponseEntity<ApiResponse<Void>> userLogin(@RequestBody Map<String, String> body) {
        service.userOnline(body.getOrDefault("username", "unknown"));
        return ResponseEntity.ok(ApiResponse.ok("用户上线"));
    }

    @PostMapping("/online/logout")
    public ResponseEntity<ApiResponse<Void>> userLogout(@RequestBody Map<String, String> body) {
        service.userOffline(body.getOrDefault("username", "unknown"));
        return ResponseEntity.ok(ApiResponse.ok("用户下线"));
    }

    @PostMapping("/online/heartbeat")
    public ResponseEntity<ApiResponse<Void>> heartbeat(@RequestBody Map<String, String> body) {
        service.userOnline(body.getOrDefault("username", "unknown"));
        return ResponseEntity.ok(ApiResponse.ok("心跳更新"));
    }

    @GetMapping("/online/check/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkOnline(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("online", service.isOnline(userId))));
    }
}
