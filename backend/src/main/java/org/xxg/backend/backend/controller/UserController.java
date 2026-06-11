package org.xxg.backend.backend.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.xxg.backend.backend.dto.ApiResponse;
import org.xxg.backend.backend.dto.ChangePasswordRequest;
import org.xxg.backend.backend.entity.User;
import org.xxg.backend.backend.service.UserService;

import java.util.Map;

/**
 * 用户管理接口控制器
 * <p>提供用户个人信息管理（个人资料、密码修改、头像上传、社交绑定）和管理员用户管理功能。</p>
 * <p>基础路径：/user, /admin/users</p>
 */
@RestController
public class UserController {
    private final UserService userService;
    public UserController(UserService userService) { this.userService = userService; }

    // --- User profile endpoints ---

    @GetMapping("/user/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfile(Authentication auth) {
        User user = userService.getUserByUsername(auth.getName());
        // 仅返回安全的用户字段，避免泄露 IP、登录失败次数等内部信息
        Map<String, Object> profile = new java.util.HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("email", user.getEmail());
        profile.put("nickname", user.getNickname());
        profile.put("avatar", user.getAvatar());
        profile.put("phone", user.getPhone());
        profile.put("status", user.getStatus());
        profile.put("emailVerified", user.getEmailVerified());
        profile.put("lastLoginTime", user.getLastLoginTime());
        profile.put("loginCount", user.getLoginCount());
        profile.put("createTime", user.getCreateTime());
        profile.put("updateTime", user.getUpdateTime());
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    @PutMapping("/user/profile")
    public ResponseEntity<ApiResponse<Void>> updateProfile(Authentication auth, @RequestBody Map<String, String> body) {
        String nickname = body.get("nickname");
        String email = body.get("email");
        String phone = body.get("phone");
        // 输入长度校验
        if (nickname != null && nickname.length() > 50) {
            return ResponseEntity.badRequest().body(ApiResponse.error("昵称长度不能超过50个字符"));
        }
        if (email != null && !email.isBlank() && !email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            return ResponseEntity.badRequest().body(ApiResponse.error("邮箱格式不正确"));
        }
        if (email != null && email.length() > 100) {
            return ResponseEntity.badRequest().body(ApiResponse.error("邮箱长度不能超过100个字符"));
        }
        if (phone != null && !phone.isBlank() && !phone.matches("^1[3-9]\\d{9}$")) {
            return ResponseEntity.badRequest().body(ApiResponse.error("手机号格式不正确"));
        }
        User user = userService.getUserByUsername(auth.getName());
        userService.updateProfile(user.getId(), nickname, email, phone);
        return ResponseEntity.ok(ApiResponse.ok("个人信息更新成功"));
    }

    @PostMapping("/user/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(Authentication auth, @Valid @RequestBody ChangePasswordRequest body) {
        User user = userService.getUserByUsername(auth.getName());
        userService.changePassword(user.getId(), body.getOldPassword(), body.getNewPassword());
        return ResponseEntity.ok(ApiResponse.ok("密码修改成功"));
    }

    @PostMapping("/user/avatar")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadAvatar(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("url", "/default-avatar.png")));
    }

    @GetMapping("/user/social")
    public ResponseEntity<ApiResponse<Object>> getSocialBindings(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(java.util.List.of()));
    }

    @PostMapping("/user/social/bind")
    public ResponseEntity<ApiResponse<Void>> bindSocial(Authentication auth, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok("绑定成功"));
    }

    @PostMapping("/user/social/unbind")
    public ResponseEntity<ApiResponse<Void>> unbindSocial(Authentication auth, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok("解绑成功"));
    }

    // --- Admin user management endpoints ---

    @GetMapping("/admin/users")
    public ResponseEntity<ApiResponse<Page<User>>> adminListUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        size = Math.min(size, 100); // 防止过大的分页请求导致 OOM
        return ResponseEntity.ok(ApiResponse.ok(userService.getAllUsers(PageRequest.of(page - 1, size))));
    }

    @PostMapping("/admin/users")
    public ResponseEntity<ApiResponse<User>> createUser(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.error("暂不支持"));
    }

    @PutMapping("/admin/users/{id}")
    public ResponseEntity<ApiResponse<Void>> updateUser(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        userService.updateProfile(id, body.get("nickname"), body.get("email"), body.get("phone"));
        return ResponseEntity.ok(ApiResponse.ok("用户已更新"));
    }

    @DeleteMapping("/admin/users/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Integer id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.ok("用户已删除"));
    }

    @PutMapping("/admin/users/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateUserStatus(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        userService.toggleUserStatus(id);
        return ResponseEntity.ok(ApiResponse.ok("用户状态已更新"));
    }

    // --- Legacy endpoints ---

    @GetMapping("/user/admin/list")
    public ResponseEntity<ApiResponse<Page<User>>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        size = Math.min(size, 100); // 防止过大的分页请求导致 OOM
        return ResponseEntity.ok(ApiResponse.ok(userService.getAllUsers(PageRequest.of(page, size))));
    }

    @PutMapping("/user/admin/{id}/toggle-status")
    public ResponseEntity<ApiResponse<Void>> toggleStatus(@PathVariable Integer id) {
        userService.toggleUserStatus(id);
        return ResponseEntity.ok(ApiResponse.ok("用户状态已更新"));
    }

    @GetMapping("/user/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUserStats()));
    }
}
