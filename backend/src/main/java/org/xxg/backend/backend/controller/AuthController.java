package org.xxg.backend.backend.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.xxg.backend.backend.dto.*;
import org.xxg.backend.backend.entity.Admin;
import org.xxg.backend.backend.entity.User;
import org.xxg.backend.backend.exception.BusinessException;
import org.xxg.backend.backend.mapper.AdminRepository;
import org.xxg.backend.backend.mapper.UserRepository;
import org.xxg.backend.backend.service.AuthService;
import org.xxg.backend.backend.service.TotpService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final TotpService totpService;

    public AuthController(AuthService authService, UserRepository userRepository,
                          AdminRepository adminRepository, TotpService totpService) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
        this.totpService = totpService;
    }

    @PostMapping("/admin/login")
    public ResponseEntity<ApiResponse<LoginResponse>> adminLogin(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.adminLogin(request)));
    }

    @PostMapping("/user/login")
    public ResponseEntity<ApiResponse<LoginResponse>> userLogin(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.userLogin(request)));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(ApiResponse.ok("注册成功"));
    }

    @PostMapping("/register-bind")
    public ResponseEntity<ApiResponse<Void>> registerBind(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(ApiResponse.ok("注册成功"));
    }

    @PostMapping("/email-code")
    public ResponseEntity<ApiResponse<Void>> sendEmailCode(@Valid @RequestBody EmailCodeRequest request) {
        authService.sendEmailCode(request.getEmail(), request.getType());
        return ResponseEntity.ok(ApiResponse.ok("验证码已发送"));
    }

    @PostMapping("/reset-code")
    public ResponseEntity<ApiResponse<Void>> sendResetCode(@Valid @RequestBody EmailCodeRequest request) {
        authService.sendEmailCode(request.getEmail(), "reset");
        return ResponseEntity.ok(ApiResponse.ok("验证码已发送"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("密码重置成功"));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(@RequestBody TokenRefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refreshToken(request)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(Authentication auth) {
        if (auth != null) {
            String username = auth.getName();
            // 清除用户表中的 token
            userRepository.findByUsername(username).ifPresent(user -> {
                user.setAccessToken(null);
                user.setRefreshToken(null);
                userRepository.save(user);
            });
        }
        return ResponseEntity.ok(ApiResponse.ok("已退出"));
    }

    @GetMapping("/user/info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserInfo(Authentication auth) {
        if (auth == null) return ResponseEntity.ok(ApiResponse.error("未登录"));
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new BusinessException("用户不存在"));
        Map<String, Object> info = new java.util.HashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("email", user.getEmail());
        info.put("nickname", user.getNickname());
        info.put("avatar", user.getAvatar());
        info.put("role", "user");
        return ResponseEntity.ok(ApiResponse.ok(info));
    }

    @PostMapping("/admin/update")
    public ResponseEntity<ApiResponse<Void>> updateAdmin(Authentication auth, @RequestBody Map<String, Object> body) {
        String username = auth.getName();
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("管理员不存在"));
        if (body.containsKey("email")) {
            admin.setEmail((String) body.get("email"));
        }
        adminRepository.save(admin);
        return ResponseEntity.ok(ApiResponse.ok("更新成功"));
    }

    @GetMapping("/bind/token")
    public ResponseEntity<ApiResponse<?>> getBindToken() {
        return ResponseEntity.ok(ApiResponse.ok(authService.getBindToken()));
    }

    @PostMapping("/bind/validate")
    public ResponseEntity<ApiResponse<Void>> validateBindToken(@RequestBody RegisterBindRequest request) {
        boolean result = authService.validateBindToken(request.getUserId(), request.getToken());
        return ResponseEntity.ok(result ? ApiResponse.ok("验证成功") : ApiResponse.error("验证失败"));
    }

    @PostMapping("/totp/setup")
    public ResponseEntity<ApiResponse<Map<String, String>>> setupTotp(Authentication auth) {
        String username = auth.getName();
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("管理员不存在"));
        String secret = totpService.generateSecret();
        admin.setTotpSecret(secret);
        adminRepository.save(admin);
        Map<String, String> result = new HashMap<>();
        result.put("secret", secret);
        result.put("qrCode", "otpauth://totp/xxgkami:" + username + "?secret=" + secret + "&issuer=xxgkami");
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/totp/enable")
    public ResponseEntity<ApiResponse<Void>> enableTotp(Authentication auth, @RequestBody Map<String, String> body) {
        String username = auth.getName();
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("管理员不存在"));
        if (admin.getTotpSecret() == null) {
            throw new BusinessException("请先设置 TOTP");
        }
        String code = body.get("code");
        if (!totpService.verifyCode(admin.getTotpSecret(), code)) {
            throw new BusinessException("验证码错误");
        }
        admin.setTotpEnabled(true);
        adminRepository.save(admin);
        return ResponseEntity.ok(ApiResponse.ok("TOTP 已启用"));
    }

    @PostMapping("/totp/disable")
    public ResponseEntity<ApiResponse<Void>> disableTotp(Authentication auth, @RequestBody Map<String, String> body) {
        String username = auth.getName();
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("管理员不存在"));
        String code = body.get("code");
        if (code == null || !totpService.verifyCode(admin.getTotpSecret(), code)) {
            throw new BusinessException("验证码错误");
        }
        admin.setTotpEnabled(false);
        admin.setTotpSecret(null);
        adminRepository.save(admin);
        return ResponseEntity.ok(ApiResponse.ok("TOTP 已禁用"));
    }

    @PostMapping("/totp/recovery-code")
    public ResponseEntity<ApiResponse<Map<String, String>>> sendRecoveryCode(@RequestBody Map<String, String> body) {
        return ResponseEntity.status(501).body(ApiResponse.error("恢复码功能暂未实现"));
    }

    @PostMapping("/totp/disable-by-recovery")
    public ResponseEntity<ApiResponse<Void>> disableTotpByRecovery(@RequestBody Map<String, String> body) {
        return ResponseEntity.status(501).body(ApiResponse.error("恢复码功能暂未实现"));
    }
}
