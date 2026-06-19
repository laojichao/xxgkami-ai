package org.xxg.backend.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.xxg.backend.backend.dto.*;
import org.xxg.backend.backend.entity.Admin;
import org.xxg.backend.backend.entity.OAuthState;
import org.xxg.backend.backend.entity.User;
import org.xxg.backend.backend.exception.BusinessException;
import org.xxg.backend.backend.mapper.AdminRepository;
import org.xxg.backend.backend.mapper.OAuthStateRepository;
import org.xxg.backend.backend.mapper.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xxg.backend.backend.service.AuthService;
import org.xxg.backend.backend.service.EmailService;
import org.xxg.backend.backend.service.TotpService;
import org.xxg.backend.backend.util.JwtUtil;
import org.xxg.backend.backend.filter.JwtRequestFilter;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 认证接口控制器。
 * <p>提供管理员/用户登录、注册、邮箱验证码、密码重置、Token 刷新与注销、
 * TOTP 两步验证设置等认证相关 API。</p>
 * <p>基础路径：{@code /auth}</p>
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "认证接口", description = "登录、注册、Token 刷新、TOTP、OAuth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final TotpService totpService;
    private final JwtUtil jwtUtil;
    private final JwtRequestFilter jwtRequestFilter;
    private final OAuthStateRepository oauthStateRepository;
    private final EmailService emailService;

    /** OAuth state 过期时间：5 分钟 */
    private static final int OAUTH_STATE_TTL_MINUTES = 5;

    /** 定时清理过期的 OAuth state */
    private final ScheduledExecutorService stateCleanup = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "oauth-state-cleanup");
        t.setDaemon(true);
        return t;
    });

    public AuthController(AuthService authService, UserRepository userRepository,
                          AdminRepository adminRepository, TotpService totpService,
                          JwtUtil jwtUtil, JwtRequestFilter jwtRequestFilter,
                          OAuthStateRepository oauthStateRepository, EmailService emailService) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
        this.totpService = totpService;
        this.jwtUtil = jwtUtil;
        this.jwtRequestFilter = jwtRequestFilter;
        this.oauthStateRepository = oauthStateRepository;
        this.emailService = emailService;
        // 每 2 分钟清理过期 state
        stateCleanup.scheduleAtFixedRate(this::cleanupExpiredStates, 2, 2, TimeUnit.MINUTES);
    }

    /** 应用关闭时清理定时器线程，防止资源泄漏 */
    @PreDestroy
    public void destroy() {
        stateCleanup.shutdownNow();
        try {
            // 安全修复：等待终止，确保所有进行中的清理任务完成
            if (!stateCleanup.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("OAuth state 清理线程未在 5 秒内终止");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("OAuth state 清理线程终止等待被中断");
        }
    }

    /** 清理过期的 OAuth state 记录 */
    private void cleanupExpiredStates() {
        try {
            oauthStateRepository.deleteByExpireTimeBefore(LocalDateTime.now());
        } catch (Exception e) {
            // 安全修复：清理失败记录 warn 日志，便于排查问题
            log.warn("OAuth state 清理失败: {}", e.getMessage());
        }
    }

    /**
     * 管理员登录。
     * <p>登录成功后将 JWT Token 写入 httpOnly Cookie，同时在响应体中保留 token 字段以兼容旧客户端。</p>
     *
     * @param request  登录请求（用户名/密码）
     * @param response HTTP 响应（用于设置 Cookie）
     * @return 包含 JWT Token 的登录响应
     */
    @Operation(summary = "管理员登录", description = "管理员使用用户名/密码登录，支持 TOTP 二次验证。登录成功后 Token 写入 httpOnly Cookie。")
    @PostMapping("/admin/login")
    public ResponseEntity<ApiResponse<LoginResponse>> adminLogin(@Valid @RequestBody LoginRequest request,
                                                                  HttpServletRequest servletRequest,
                                                                  HttpServletResponse response) {
        LoginResponse loginResponse = authService.adminLogin(request);
        setTokenCookies(response, loginResponse.getToken(), loginResponse.getRefreshToken());
        // 移动端客户端需要从响应体中获取 Token，Web端使用 httpOnly Cookie
        if (!isMobileClient(servletRequest)) {
            loginResponse.setToken(null);
            loginResponse.setRefreshToken(null);
        }
        return ResponseEntity.ok(ApiResponse.ok(loginResponse));
    }

    /**
     * 普通用户登录。
     * <p>登录成功后将 JWT Token 写入 httpOnly Cookie，同时在响应体中保留 token 字段以兼容旧客户端。</p>
     *
     * @param request  登录请求（用户名/密码）
     * @param response HTTP 响应（用于设置 Cookie）
     * @return 包含 JWT Token 的登录响应
     */
    @Operation(summary = "用户登录", description = "普通用户使用用户名/密码登录。登录成功后 Token 写入 httpOnly Cookie。")
    @PostMapping("/user/login")
    public ResponseEntity<ApiResponse<LoginResponse>> userLogin(@Valid @RequestBody LoginRequest request,
                                                                 HttpServletRequest servletRequest,
                                                                 HttpServletResponse response) {
        LoginResponse loginResponse = authService.userLogin(request);
        setTokenCookies(response, loginResponse.getToken(), loginResponse.getRefreshToken());
        // 移动端客户端需要从响应体中获取 Token，Web端使用 httpOnly Cookie
        if (!isMobileClient(servletRequest)) {
            loginResponse.setToken(null);
            loginResponse.setRefreshToken(null);
        }
        return ResponseEntity.ok(ApiResponse.ok(loginResponse));
    }

    /**
     * 用户注册。
     *
     * @param request 注册请求
     * @return 操作结果
     */
    @Operation(summary = "用户注册", description = "新用户注册，需要邮箱验证码。密码要求：8-50位，包含大小写字母和数字。")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(ApiResponse.ok("注册成功"));
    }

    /**
     * 注册并绑定（客户端绑定场景）。
     * @deprecated 此接口与 /auth/register 功能完全相同，请使用 /auth/register 替代。
     * 保留此接口仅为兼容旧版客户端，后续版本将移除。
     */
    @Deprecated(since = "1.0.2", forRemoval = true)
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
     * <p>优先从 httpOnly Cookie 中读取 refresh_token，若不存在则使用请求体中的 refreshToken（兼容旧客户端）。
     * 刷新成功后更新 Cookie。</p>
     *
     * @param request  包含 refresh_token 的请求（可选，Cookie 优先）
     * @param servletRequest  HTTP 请求（用于读取 Cookie）
     * @param response HTTP 响应（用于更新 Cookie）
     * @return 新的 Token 对
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @RequestBody(required = false) TokenRefreshRequest request,
            jakarta.servlet.http.HttpServletRequest servletRequest,
            HttpServletResponse response) {
        // 优先从 Cookie 中读取 refresh_token
        String refreshTokenValue = null;
        if (servletRequest.getCookies() != null) {
            for (Cookie cookie : servletRequest.getCookies()) {
                if ("refresh_token".equals(cookie.getName())) {
                    refreshTokenValue = cookie.getValue();
                    break;
                }
            }
        }
        // Cookie 中不存在时，从请求体中读取（兼容旧客户端）
        if (refreshTokenValue == null) {
            if (request == null || request.getRefreshToken() == null) {
                throw new BusinessException("refreshToken 不能为空");
            }
            refreshTokenValue = request.getRefreshToken();
        }
        TokenRefreshRequest refreshRequest = new TokenRefreshRequest();
        refreshRequest.setRefreshToken(refreshTokenValue);
        LoginResponse loginResponse = authService.refreshToken(refreshRequest);
        setTokenCookies(response, loginResponse.getToken(), loginResponse.getRefreshToken());
        // 移动端客户端需要从响应体中获取 Token，Web端使用 httpOnly Cookie
        if (!isMobileClient(servletRequest)) {
            loginResponse.setToken(null);
            loginResponse.setRefreshToken(null);
        }
        return ResponseEntity.ok(ApiResponse.ok(loginResponse));
    }

    /**
     * 用户注销，清除服务端存储的 Token 并清除客户端 httpOnly Cookie。
     *
     * @param auth    当前认证信息
     * @param response HTTP 响应（用于清除 Cookie）
     * @return 操作结果
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(Authentication auth, HttpServletResponse response) {
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
            // Invalidate the JWT filter cache so the next request checks the DB immediately
            // instead of waiting for the 30-second cache TTL
            String role = auth.getAuthorities().stream()
                    .findFirst()
                    .map(a -> a.getAuthority().replace("ROLE_", "").toLowerCase())
                    .orElse("user");
            jwtRequestFilter.invalidateAccountCache(username, role);
        }
        // 清除客户端 httpOnly Cookie
        clearTokenCookies(response);
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
        if (auth == null) return ResponseEntity.status(401).body(ApiResponse.error("未登录"));
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
     * @param request 更新请求（含邮箱字段）
     * @return 操作结果
     */
    @PostMapping("/admin/update")
    public ResponseEntity<ApiResponse<Void>> updateAdmin(Authentication auth,
                                                          @Valid @RequestBody UpdateAdminRequest request) {
        if (auth == null) return ResponseEntity.status(401).body(ApiResponse.error("未登录"));
        String username = auth.getName();
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("管理员不存在"));
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            admin.setEmail(request.getEmail());
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
    public ResponseEntity<ApiResponse<Void>> validateBindToken(@Valid @RequestBody RegisterBindRequest request) {
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
        // 防止已启用 TOTP 的管理员被重置密钥
        if (Boolean.TRUE.equals(admin.getTotpEnabled())) {
            throw new BusinessException("TOTP 已启用，请先禁用后再重新配置");
        }
        String secret = totpService.generateSecret();
        admin.setTotpSecret(totpService.encryptSecret(secret));
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
     * @param request 包含 TOTP 验证码的请求
     * @return 操作结果
     */
    @PostMapping("/totp/enable")
    public ResponseEntity<ApiResponse<Void>> enableTotp(Authentication auth,
                                                         @Valid @RequestBody TotpCodeRequest request) {
        if (auth == null) return ResponseEntity.status(401).body(ApiResponse.error("未登录"));
        String username = auth.getName();
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("管理员不存在"));
        if (admin.getTotpSecret() == null) {
            throw new BusinessException("请先设置 TOTP");
        }
        String decryptedSecret = totpService.decryptSecret(admin.getTotpSecret());
        if (!totpService.verifyCode(decryptedSecret, request.getCode())) {
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
     * @param request 包含 TOTP 验证码的请求
     * @return 操作结果
     */
    @PostMapping("/totp/disable")
    public ResponseEntity<ApiResponse<Void>> disableTotp(Authentication auth,
                                                          @Valid @RequestBody TotpCodeRequest request) {
        if (auth == null) return ResponseEntity.status(401).body(ApiResponse.error("未登录"));
        String username = auth.getName();
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("管理员不存在"));
        if (admin.getTotpSecret() == null) {
            throw new BusinessException("TOTP 未启用");
        }
        String decryptedSecret = totpService.decryptSecret(admin.getTotpSecret());
        if (!totpService.verifyCode(decryptedSecret, request.getCode())) {
            throw new BusinessException("验证码错误");
        }
        admin.setTotpEnabled(false);
        admin.setTotpSecret(null);
        adminRepository.save(admin);
        return ResponseEntity.ok(ApiResponse.ok("TOTP 已禁用"));
    }

    /**
     * 生成 TOTP 恢复码。
     */
    /**
     * 生成 TOTP 恢复码。
     * <p>生成 8 个一次性恢复码，哈希后存储到数据库，明文通过邮件发送给管理员。
     * 每个恢复码为 8 位字母数字组合。</p>
     *
     * @param auth 当前认证信息
     * @return 恢复码列表（仅此次返回，后续不可再查看）
     */
    @PostMapping("/totp/recovery-code")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendRecoveryCode(Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).body(ApiResponse.error("未登录"));
        Admin admin = adminRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new BusinessException("管理员不存在"));
        if (!Boolean.TRUE.equals(admin.getTotpEnabled())) {
            throw new BusinessException("TOTP 未启用，无需生成恢复码");
        }
        // 生成 8 个恢复码
        List<String> codes = new ArrayList<>();
        List<String> hashedCodes = new ArrayList<>();
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < 8; i++) {
            String code = generateRecoveryCode(random);
            codes.add(code);
            hashedCodes.add(hashRecoveryCode(code));
        }
        // 存储哈希后的恢复码
        admin.setTotpRecoveryCodes(String.join(",", hashedCodes));
        adminRepository.save(admin);
        // 通过邮件发送恢复码
        boolean emailSent = false;
        try {
            emailService.sendVerificationCodeSync(admin.getEmail(),
                    String.join("\n", codes), "recovery");
            emailSent = true;
        } catch (Exception e) {
            log.warn("恢复码邮件发送失败: {}", e.getMessage());
        }
        Map<String, Object> result = new HashMap<>();
        // 安全修复：邮件发送成功时不在响应体返回恢复码，仅返回成功提示
        // 邮件发送失败时才在响应体返回恢复码，避免管理员丢失恢复码
        if (emailSent) {
            result.put("message", "恢复码已通过邮件发送至管理员邮箱，请查收。每个恢复码只能使用一次。");
        } else {
            result.put("recoveryCodes", codes);
            result.put("message", "恢复码邮件发送失败，请妥善保管以下恢复码。每个恢复码只能使用一次。");
        }
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 通过恢复码禁用 TOTP。
     * <p>验证恢复码后禁用 TOTP 并清除所有恢复码。</p>
     *
     * @param body 请求体，包含 username 和 recoveryCode
     * @return 操作结果
     */
    @PostMapping("/totp/disable-by-recovery")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> disableTotpByRecovery(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String recoveryCode = body.get("recoveryCode");
        if (username == null || username.isBlank() || recoveryCode == null || recoveryCode.isBlank()) {
            throw new BusinessException("用户名和恢复码不能为空");
        }
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("管理员不存在"));
        if (!Boolean.TRUE.equals(admin.getTotpEnabled())) {
            throw new BusinessException("TOTP 未启用");
        }
        String storedCodes = admin.getTotpRecoveryCodes();
        if (storedCodes == null || storedCodes.isBlank()) {
            throw new BusinessException("恢复码不存在，请联系系统管理员");
        }
        // 验证恢复码（使用常量时间比较防止时序攻击）
        String hashedInput = hashRecoveryCode(recoveryCode.trim());
        List<String> hashedCodes = new ArrayList<>(Arrays.asList(storedCodes.split(",")));
        boolean matched = false;
        String matchedCode = null;
        for (String hashed : hashedCodes) {
            if (MessageDigest.isEqual(
                    hashed.getBytes(StandardCharsets.UTF_8),
                    hashedInput.getBytes(StandardCharsets.UTF_8))) {
                matched = true;
                matchedCode = hashed;
                break;
            }
        }
        if (!matched) {
            throw new BusinessException("恢复码无效或已使用");
        }
        hashedCodes.remove(matchedCode);
        // 禁用 TOTP 并更新剩余恢复码
        admin.setTotpEnabled(false);
        admin.setTotpSecret(null);
        admin.setTotpRecoveryCodes(hashedCodes.isEmpty() ? null : String.join(",", hashedCodes));
        adminRepository.save(admin);
        return ResponseEntity.ok(ApiResponse.ok("TOTP 已禁用"));
    }

    /** 生成 8 位随机恢复码 */
    private String generateRecoveryCode(SecureRandom random) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // 排除易混淆字符
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /** 对恢复码进行 SHA-256 哈希 */
    private String hashRecoveryCode(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(code.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("恢复码哈希失败", e);
        }
    }

    /**
     * 生成 OAuth state nonce（前端发起 OAuth 前调用）。
     * <p>返回一个一次性的 state 参数，前端必须在 OAuth 回调时
     * 将此 state 传递给 /auth/oauth/set-cookies 接口。</p>
     * <p>有效期 5 分钟，使用后自动失效。</p>
     */
    @GetMapping("/oauth/state")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, String>>> generateOAuthState() {
        String state = UUID.randomUUID().toString();
        OAuthState oauthState = new OAuthState();
        oauthState.setState(state);
        oauthState.setExpireTime(LocalDateTime.now().plusMinutes(OAUTH_STATE_TTL_MINUTES));
        oauthStateRepository.save(oauthState);
        Map<String, String> result = new HashMap<>();
        result.put("state", state);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * OAuth 回调后将 URL 参数中的 Token 设置为 httpOnly Cookie。
     * <p>OAuth 第三方登录成功后，Token 通过 URL 参数传递到前端，
     * 前端调用此接口将 Token 安全地写入 httpOnly Cookie，
     * 避免 Token 暴露在浏览器历史记录和 JavaScript 中。</p>
     * <p>安全要求：必须携带有效的 state 参数（从 /auth/oauth/state 获取），
     * 防止攻击者利用任意有效 JWT 进行 session fixation 攻击。</p>
     *
     * @param body     包含 token、refreshToken 和 state 的请求体
     * @param response HTTP 响应（用于设置 Cookie）
     * @return 操作结果
     */
    @PostMapping("/oauth/set-cookies")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> setOAuthCookies(@RequestBody Map<String, String> body,
                                                              HttpServletResponse response) {
        // 验证 state 参数（防止 session fixation）
        String state = body.get("state");
        if (state == null || state.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("缺少 state 参数"));
        }
        // 从数据库中查找并删除 state（一次性使用）
        OAuthState oauthState = oauthStateRepository.findByStateAndExpireTimeAfter(state, LocalDateTime.now())
                .orElse(null);
        if (oauthState == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("无效或已过期的 state 参数"));
        }
        // 立即删除，防止重复使用
        oauthStateRepository.delete(oauthState);

        String accessToken = body.get("token");
        String refreshTokenValue = body.get("refreshToken");
        if (accessToken == null || refreshTokenValue == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("token 和 refreshToken 不能为空"));
        }
        // Validate both tokens before setting cookies
        if (!jwtUtil.isTokenValid(accessToken) || !jwtUtil.isAccessToken(accessToken)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("无效的 access token"));
        }
        if (!jwtUtil.isTokenValid(refreshTokenValue) || !jwtUtil.isRefreshToken(refreshTokenValue)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("无效的 refresh token"));
        }
        setTokenCookies(response, accessToken, refreshTokenValue);
        return ResponseEntity.ok(ApiResponse.ok("Cookie 已设置"));
    }

    /**
     * 判断请求是否来自移动端客户端。
     * <p>通过 User-Agent 头部识别 Android/iOS/Ktor 等移动端特征。
     * 移动端客户端需要在响应体中获取 Token（无法使用 httpOnly Cookie）。</p>
     *
     * @param request HTTP 请求
     * @return true 如果是移动端客户端
     */
    private boolean isMobileClient(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) return false;
        String ua = userAgent.toLowerCase();
        return ua.contains("android") || ua.contains("iphone") || ua.contains("ktor")
                || ua.contains("okhttp") || ua.contains("xxgkami");
    }

    /**
     * 将 access_token 和 refresh_token 写入 httpOnly Cookie。
     * <p>Cookie 属性说明：</p>
     * <ul>
     *   <li>httpOnly=true: 禁止 JavaScript 读取，防止 XSS 窃取 Token</li>
     *   <li>secure=true: 仅通过 HTTPS 传输（开发环境可通过 localhost 豁免）</li>
     *   <li>sameSite=Strict: 防止 CSRF 攻击，完全禁止跨站携带 Cookie</li>
     *   <li>path: access_token 为 "/"（全局可用），refresh_token 为 "/auth/refresh"（最小权限）</li>
     * </ul>
     */
    private void setTokenCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        ResponseCookie accessCookie = ResponseCookie.from("access_token", accessToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(3600)
                .sameSite("Strict")
                .build();
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/auth/refresh")
                .maxAge(604800)
                .sameSite("Strict")
                .build();
        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());
    }

    /**
     * 清除客户端的 access_token 和 refresh_token Cookie。
     * <p>通过将 maxAge 设为 0 使 Cookie 立即过期。</p>
     */
    private void clearTokenCookies(HttpServletResponse response) {
        ResponseCookie accessCookie = ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .path("/auth/refresh")
                .maxAge(0)
                .sameSite("Strict")
                .build();
        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());
    }
}
