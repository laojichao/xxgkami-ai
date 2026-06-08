package org.xxg.backend.backend.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xxg.backend.backend.dto.ApiResponse;
import org.xxg.backend.backend.entity.AccessLog;
import org.xxg.backend.backend.entity.IpBlacklist;
import org.xxg.backend.backend.service.SecurityService;

import java.util.List;
import java.util.Map;

/**
 * 安全管理接口
 * <p>提供IP黑名单管理和访问日志查询功能。</p>
 * <p>基础路径：/security</p>
 * <p>权限：仅管理员</p>
 */
@RestController
@RequestMapping("/security")
public class SecurityController {
    private final SecurityService securityService;
    public SecurityController(SecurityService securityService) { this.securityService = securityService; }

    /**
     * 获取IP黑名单列表
     * <p>GET /security/blacklist</p>
     * <p>权限：管理员</p>
     * @return 当前所有被封禁的IP列表
     */
    @GetMapping("/blacklist")
    public ResponseEntity<ApiResponse<List<IpBlacklist>>> getBlacklist() {
        return ResponseEntity.ok(ApiResponse.ok(securityService.getBlacklist()));
    }

    /**
     * 封禁指定IP
     * <p>POST /security/blacklist</p>
     * <p>权限：管理员</p>
     * @param body 请求体，包含 ip（IP地址，必填）、reason（封禁原因）、hours（封禁时长，小时，可选，为空则永久封禁）
     * @return 新创建的黑名单记录，IP为空时返回错误信息
     */
    @PostMapping("/blacklist")
    public ResponseEntity<ApiResponse<IpBlacklist>> blockIp(@RequestBody Map<String, Object> body) {
        Object ipObj = body.get("ip");
        String ip = ipObj != null ? ipObj.toString() : null;
        if (ip == null || ip.isBlank()) {
            return ResponseEntity.ok(ApiResponse.error("IP 地址不能为空"));
        }
        String reason = (String) body.get("reason");
        Number hoursNum = (Number) body.get("hours");
        Integer hours = hoursNum != null ? hoursNum.intValue() : null;
        return ResponseEntity.ok(ApiResponse.ok(securityService.blockIp(ip, reason, hours)));
    }

    /**
     * 解除指定IP的封禁
     * <p>DELETE /security/blacklist/{ip}</p>
     * <p>权限：管理员</p>
     * @param ip 要解封的IP地址
     * @return 操作结果
     */
    @DeleteMapping("/blacklist/{ip}")
    public ResponseEntity<ApiResponse<Void>> unblockIp(@PathVariable String ip) {
        securityService.unblockIp(ip);
        return ResponseEntity.ok(ApiResponse.ok("已解除封禁"));
    }

    /**
     * 查询访问日志（支持按IP和用户名筛选，分页）
     * <p>GET /security/access-logs</p>
     * <p>权限：管理员</p>
     * @param ip 可选参数，按IP地址筛选
     * @param username 可选参数，按用户名筛选
     * @param page 页码，默认0
     * @param size 每页条数，默认50
     * @return 分页的访问日志列表
     */
    @GetMapping("/access-logs")
    public ResponseEntity<ApiResponse<Page<AccessLog>>> getAccessLogs(
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        size = Math.min(size, 100); // 防止过大的分页请求导致 OOM
        return ResponseEntity.ok(ApiResponse.ok(
                securityService.getAccessLogs(ip, username, PageRequest.of(page, size))));
    }
}
