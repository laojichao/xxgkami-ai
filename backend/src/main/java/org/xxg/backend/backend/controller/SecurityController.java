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

@RestController
@RequestMapping("/security")
public class SecurityController {
    private final SecurityService securityService;
    public SecurityController(SecurityService securityService) { this.securityService = securityService; }

    @GetMapping("/blacklist")
    public ResponseEntity<ApiResponse<List<IpBlacklist>>> getBlacklist() {
        return ResponseEntity.ok(ApiResponse.ok(securityService.getBlacklist()));
    }

    @PostMapping("/blacklist")
    public ResponseEntity<ApiResponse<IpBlacklist>> blockIp(@RequestBody Map<String, Object> body) {
        String ip = (String) body.get("ip");
        if (ip == null || ip.isBlank()) {
            return ResponseEntity.ok(ApiResponse.error("IP 地址不能为空"));
        }
        String reason = (String) body.get("reason");
        Number hoursNum = (Number) body.get("hours");
        Integer hours = hoursNum != null ? hoursNum.intValue() : null;
        return ResponseEntity.ok(ApiResponse.ok(securityService.blockIp(ip, reason, hours)));
    }

    @DeleteMapping("/blacklist/{ip}")
    public ResponseEntity<ApiResponse<Void>> unblockIp(@PathVariable String ip) {
        securityService.unblockIp(ip);
        return ResponseEntity.ok(ApiResponse.ok("已解除封禁"));
    }

    @GetMapping("/access-logs")
    public ResponseEntity<ApiResponse<Page<AccessLog>>> getAccessLogs(
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                securityService.getAccessLogs(ip, username, PageRequest.of(page, size))));
    }
}
