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

/**
 * 认证接口控制器。
 * <p>提供管理员/用户登录、注册、邮箱验证码、密码重置、Token 刷新与注销、
 * TOTP 两步验证设置等认证相关 API。</p>
 * <p>基础路径：{@code /auth}</p>
 */
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

    /**
     * 管理员登录。
     *
     * @param request 登录请求（用户名/密码）
     * @return 包含 JWT Token 的登录响应
     */
    @PostMapping("/admin/login")
    public ResponseEntity<ApiResponse<LoginResponse>> adminLogin(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.adminLogin(request)));
    }

    /**
     * 普通用户登录。
     *
     * @param request 登录请求（用户名/密码）
     * @return 包含 JWT Token 的登录响应
     */
    @PostMapping("/user/login")
    public ResponseEntity<ApiResponse<LoginResponse>> userLogin(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.userLogin(request)));
    }

    /**
     * 用户注册。
     *
     * @param request 注册请求
     * @return 操作结果
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(ApiResponse.ok("注册成功"));
    }

    /**
     * 注册并绑定（客户端绑定场景）。
     *
     * @param request 注册请求
     * @return 操作结果
     */
    @PostMapping("/register-bind")
    public ResponseEntity<ApiResponse<Void>> registerBind(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(ApiResponse.ok("注册成功"));
    }

    /**
     * 发送邮箱验证码（注册/绑定等场景）。
     *
     * @param request 包含邮箱和验证码类型的请求
     * @return 操作结果
     */
    @PostMapping("/email-code")
    public ResponseEntity<ApiResponse<Void>> sendEmailCode(@Valid @RequestBody EmailCodeRequest request) {
        authService.sendEmailCode(request.getEmail(), request.getType());
        return ResponseEntity.ok(ApiResponse.ok("验证码已发送"));
    }

    /**
     * 发送密码重置验证码。
     *
     * @param request 包含邮箱的请求
     * @return 操作结果
     */
    @PostMapping("/reset-code")
    public ResponseEntity<ApiResponse<Void>> sendResetCode(@Valid @RequestBody EmailCodeRequest request) {
        authService.sendEmailCode(request.getEmail(), "reset");
        return ResponseEntity.ok(ApiResponse.ok("验证码已发送"));
    }

    /**
     * 重置密码。
     *
     * @param request 密码重置请求（含验证码）
     * @return 操作结果
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("密码重置成功"));
    }

    /**
     * 刷新 JWT Token。
     *
     * @param request 包含 refresh_token 的请求
     * @return 新的 Token 对
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refreshToken(request)));
    }

    /**
     * 用户注销，清除服务端存储的 Token。
     *
     * @param auth 当前认证信息
     * @return 操作结果
     */
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
            // 同时清除管理员表中的 token（管理员也可能通过此接口注销）
            adminRepository.findByUsername(username).ifPresent(admin -> {
                admin.setAccessToken(null);
                admin.setRefreshToken(null);
                adminRepository.save(admin);
            });
        }
        return ResponseEntity.ok(ApiResponse.ok("已退出"));
    }

    /**
     * 获取当前登录用户的基本信息。
     *
     * @param auth 当前认证信息
     * @return 用户信息（id、username、email、nickname、avatar、role）
     */
    @GetMapping("/user/info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserInfo(Authentication auth) {
        if (auth == null) return ResponseEntity.ok(ApiResponse.error("未登录"));
        String username = auth.getName();
        // 先查用户表，再查管理员表，兼容两种角色
        User user = userRepository.findByUsername(username).orElse(null);
        if (user != null) {
            Map<String, Object> info = new java.util.HashMap<>();
            info.put("id", user.getId());
            info.put("username", user.getUsername());
            info.put("email", user.getEmail());
            info.put("nickname", user.getNickname());
            info.put("avatar", user.getAvatar());
            info.put("role", "user");
            return ResponseEntity.ok(ApiResponse.ok(info));
        }
        Admin admin = adminRepository.findByUsername(username).orElse(null);
        if (admin != null) {
            Map<String, Object> info = new java.util.HashMap<>();
            info.put("id", admin.getId());
            info.put("username", admin.getUsername());
            info.put("email", admin.getEmail());
            info.put("nickname", admin.getUsername());
            info.put("avatar", "");
            info.put("role", "admin");
            return ResponseEntity.ok(ApiResponse.ok(info));
        }
        throw new BusinessException("用户不存在");
    }

    /**
     * 更新管理员信息（如邮箱）。
     *
     * @param auth 当前认证信息
     * @param body 待更新的字段
     * @return 操作结果
     */
    @PostMapping("/admin/update")
    public ResponseEntity<ApiResponse<Void>> updateAdmin(Authentication auth, @RequestBody Map<String, Object> body) {
        if (auth == null) return ResponseEntity.status(401).body(ApiResponse.error("未登录"));
        String username = auth.getName();
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("管理员不存在"));
        if (body.containsKey("email")) {
            Object emailObj = body.get("email");
            if (emailObj != null && !(emailObj instanceof String)) {
                return ResponseEntity.badRequest().body(ApiResponse.error("邮箱字段类型错误"));
            }
            String email = (String) emailObj;
            if (email != null && !email.isBlank() && !email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
                return ResponseEntity.badRequest().body(ApiResponse.error("邮箱格式不正确"));
            }
            admin.setEmail(email);
        }
        adminRepository.save(admin);
        return ResponseEntity.ok(ApiResponse.ok("更新成功"));
    }

    /**
     * 获取绑定 Token（用于客户端绑定流程）。
     *
     * @return 绑定 Token
     */
    @GetMapping("/bind/token")
    public ResponseEntity<ApiResponse<?>> getBindToken(Authentication auth) {
        Integer userId = null;
        if (auth != null) {
            userId = userRepository.findByUsername(auth.getName())
                    .map(User::getId).orElse(null);
        }
        return ResponseEntity.ok(ApiResponse.ok(authService.getBindToken(userId)));
    }

    /**
     * 验证绑定 Token。
     *
     * @param request 包含 userId 和 token 的请求
     * @return 验证结果
     */
    @PostMapping("/bind/validate")
    public ResponseEntity<ApiResponse<Void>> validateBindToken(@RequestBody RegisterBindRequest request) {
        boolean result = authService.validateBindToken(request.getUserId(), request.getToken());
        return ResponseEntity.ok(result ? ApiResponse.ok("验证成功") : ApiResponse.error("验证失败"));
    }

    /**
     * 初始化 TOTP 两步验证，返回密钥和二维码 URL。
     *
     * @param auth 当前认证信息
     * @return 包含 secret 和 qrCode 的结果
     */
    @PostMapping("/totp/setup")
    public ResponseEntity<ApiResponse<Map<String, String>>> setupTotp(Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).body(ApiResponse.error("未登录"));
        String username = auth.getName();
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("管理员不存在"));
        String secret = totpService.generateSecret();
        admin.setTotpSecret(secret);
        adminRepository.save(admin);
        Map<String, String> result = new HashMap<>();
        result.put("secret", secret);
        result.put("qrCode", "otpauth://totp/xxgkami:" + java.net.URLEncoder.encode(username, java.nio.charset.StandardCharsets.UTF_8) + "?secret=" + secret + "&issuer=xxgkami");
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 启用 TOTP 两步验证（需提供正确的验证码）。
     *
     * @param auth 当前认证信息
     * @param body 包含 TOTP 验证码的请求体
     * @return 操作结果
     */
    @PostMapping("/totp/enable")
    public ResponseEntity<ApiResponse<Void>> enableTotp(Authentication auth, @RequestBody Map<String, String> body) {
        if (auth == null) return ResponseEntity.status(401).body(ApiResponse.error("未登录"));
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

    /**
     * 禁用 TOTP 两步验证（需提供正确的验证码）。
     *
     * @param auth 当前认证信息
     * @param body 包含 TOTP 验证码的请求体
     * @return 操作结果
     */
    @PostMapping("/totp/disable")
    public ResponseEntity<ApiResponse<Void>> disableTotp(Authentication auth, @RequestBody Map<String, String> body) {
        if (auth == null) return ResponseEntity.status(401).body(ApiResponse.error("未登录"));
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

    /**
     * 发送 TOTP 恢复码（暂未实现）。
     */
    @PostMapping("/totp/recovery-code")
    public ResponseEntity<ApiResponse<Map<String, String>>> sendRecoveryCode(@RequestBody Map<String, String> body) {
        return ResponseEntity.status(501).body(ApiResponse.error("恢复码功能暂未实现"));
    }

    /**
     * 通过恢复码禁用 TOTP（暂未实现）。
     */
    @PostMapping("/totp/disable-by-recovery")
    public ResponseEntity<ApiResponse<Void>> disableTotpByRecovery(@RequestBody Map<String, String> body) {
        return ResponseEntity.status(501).body(ApiResponse.error("恢复码功能暂未实现"));
    }
}
