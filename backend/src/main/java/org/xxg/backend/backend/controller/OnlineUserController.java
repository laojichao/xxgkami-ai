package org.xxg.backend.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.xxg.backend.backend.dto.ApiResponse;
import org.xxg.backend.backend.service.OnlineUserService;
import java.util.*;

/**
 * 在线用户管理接口
 * <p>提供在线用户的查询、登录/登出状态管理及心跳检测功能。</p>
 * <p>基础路径：/online-users、/online/*</p>
 */
@RestController
public class OnlineUserController {
    private final OnlineUserService service;
    public OnlineUserController(OnlineUserService service) { this.service = service; }

    /**
     * 获取当前在线用户列表
     * <p>GET /online-users 或 /online/list</p>
     * <p>权限：公开访问</p>
     * @return 包含 count（在线人数）和 users（用户列表）的信息
     */
    @GetMapping({"/online-users", "/online/list"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOnlineUsers() {
        Map<String, Object> result = new HashMap<>();
        result.put("count", service.getOnlineCount());
        result.put("users", service.getOnlineUsers());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 用户登录上线
     * <p>POST /online/login</p>
     * <p>权限：已认证用户</p>
     * <p>安全修复：从 Authentication 对象获取用户名，防止身份伪造</p>
     * @param auth 当前认证信息
     * @return 操作结果
     */
    @PostMapping("/online/login")
    public ResponseEntity<ApiResponse<Void>> userLogin(Authentication auth) {
        String username = (auth != null) ? auth.getName() : "unknown";
        service.userOnline(username);
        return ResponseEntity.ok(ApiResponse.ok("用户上线"));
    }

    /**
     * 用户登出下线
     * <p>POST /online/logout</p>
     * <p>权限：已认证用户</p>
     * <p>安全修复：从 Authentication 对象获取用户名，防止身份伪造</p>
     * @param auth 当前认证信息
     * @return 操作结果
     */
    @PostMapping("/online/logout")
    public ResponseEntity<ApiResponse<Void>> userLogout(Authentication auth) {
        String username = (auth != null) ? auth.getName() : "unknown";
        service.userOffline(username);
        return ResponseEntity.ok(ApiResponse.ok("用户下线"));
    }

    /**
     * 用户心跳保活
     * <p>POST /online/heartbeat</p>
     * <p>权限：已认证用户</p>
     * <p>安全修复：从 Authentication 对象获取用户名，防止身份伪造</p>
     * @param auth 当前认证信息
     * @return 操作结果
     */
    @PostMapping("/online/heartbeat")
    public ResponseEntity<ApiResponse<Void>> heartbeat(Authentication auth) {
        String username = (auth != null) ? auth.getName() : "unknown";
        service.userOnline(username);
        return ResponseEntity.ok(ApiResponse.ok("心跳更新"));
    }

    /**
     * 检查指定用户是否在线
     * <p>GET /online/check/{userId}</p>
     * <p>权限：公开访问</p>
     * @param userId 用户ID
     * @return 包含 online 布尔值的查询结果
     */
    @GetMapping("/online/check/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkOnline(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("online", service.isOnline(userId))));
    }
}
