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

@RestController
public class UserController {
    private final UserService userService;
    public UserController(UserService userService) { this.userService = userService; }

    // --- User profile endpoints ---

    @GetMapping("/user/profile")
    public ResponseEntity<ApiResponse<User>> getProfile(Authentication auth) {
        User user = userService.getUserByUsername(auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @PutMapping("/user/profile")
    public ResponseEntity<ApiResponse<Void>> updateProfile(Authentication auth, @RequestBody Map<String, String> body) {
        User user = userService.getUserByUsername(auth.getName());
        userService.updateProfile(user.getId(), body.get("nickname"), body.get("email"), body.get("phone"));
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
