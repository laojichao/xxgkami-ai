package org.xxg.backend.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xxg.backend.backend.dto.*;
import org.xxg.backend.backend.entity.*;
import org.xxg.backend.backend.exception.BusinessException;
import org.xxg.backend.backend.mapper.*;
import org.xxg.backend.backend.util.JwtUtil;
import org.xxg.backend.backend.util.PasswordUtil;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 认证服务 - 处理用户/管理员的登录、注册、验证码和令牌刷新等认证相关业务逻辑
 * <p>主要功能包括:</p>
 * <ul>
 *   <li>管理员登录认证</li>
 *   <li>用户登录认证(含账号状态校验)</li>
 *   <li>用户注册(含邮箱验证码校验)</li>
 *   <li>邮箱验证码发送与管理</li>
 *   <li>JWT令牌刷新</li>
 *   <li>密码重置(含验证码校验)</li>
 *   <li>绑定令牌管理(第三方登录场景)</li>
 * </ul>
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    /** 登录失败锁定阈值 */
    private static final int MAX_FAILED_ATTEMPTS = 5;
    /** 账户锁定时长（分钟） */
    private static final int LOCKOUT_DURATION_MINUTES = 15;
    /** 验证码有效期（分钟） */
    private static final int CODE_EXPIRE_MINUTES = 10;
    /** 验证码发送冷却时间（秒） */
    private static final int CODE_SEND_COOLDOWN_SECONDS = 60;

    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final VerificationCodeRepository verificationCodeRepository;
    private final BindTokenRepository bindTokenRepository;
    private final JwtUtil jwtUtil;
    private final PasswordUtil passwordUtil;
    private final EmailService emailService;
    private final TotpService totpService;

    public AuthService(AdminRepository adminRepository, UserRepository userRepository,
                       VerificationCodeRepository verificationCodeRepository,
                       BindTokenRepository bindTokenRepository,
                       JwtUtil jwtUtil, PasswordUtil passwordUtil, EmailService emailService,
                       TotpService totpService) {
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
        this.verificationCodeRepository = verificationCodeRepository;
        this.bindTokenRepository = bindTokenRepository;
        this.jwtUtil = jwtUtil;
        this.passwordUtil = passwordUtil;
        this.emailService = emailService;
        this.totpService = totpService;
    }

    @Transactional
    public LoginResponse adminLogin(LoginRequest request) {
        Admin admin = adminRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));

        // Check account lockout
        if (admin.getFailedLoginAttempts() != null && admin.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS
                && admin.getLockTime() != null
                && admin.getLockTime().plusMinutes(LOCKOUT_DURATION_MINUTES).isAfter(LocalDateTime.now())) {
            throw new BusinessException("账户已锁定，请" + LOCKOUT_DURATION_MINUTES + "分钟后重试");
        }
        // If lock has expired, reset attempts
        if (admin.getFailedLoginAttempts() != null && admin.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS
                && admin.getLockTime() != null
                && admin.getLockTime().plusMinutes(LOCKOUT_DURATION_MINUTES).isBefore(LocalDateTime.now())) {
            admin.setFailedLoginAttempts(0);
            admin.setLockTime(null);
        }

        if (!passwordUtil.matches(request.getPassword(), admin.getPassword())) {
            // Record failed attempt
            int attempts = (admin.getFailedLoginAttempts() == null ? 0 : admin.getFailedLoginAttempts()) + 1;
            admin.setFailedLoginAttempts(attempts);
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                admin.setLockTime(LocalDateTime.now());
            }
            adminRepository.save(admin);
            throw new BusinessException("用户名或密码错误");
        }

        // Reset failed attempts on successful login
        if (admin.getFailedLoginAttempts() != null && admin.getFailedLoginAttempts() > 0) {
            admin.setFailedLoginAttempts(0);
            admin.setLockTime(null);
        }

        // 强制 TOTP 二次验证：管理员启用 TOTP 后必须提供验证码
        if (Boolean.TRUE.equals(admin.getTotpEnabled())) {
            if (request.getTotpCode() == null || request.getTotpCode().isBlank()) {
                throw new BusinessException("请输入 TOTP 验证码");
            }
            String decryptedSecret = totpService.decryptSecret(admin.getTotpSecret());
            if (!totpService.verifyCode(decryptedSecret, request.getTotpCode())) {
                throw new BusinessException("TOTP 验证码错误");
            }
        }

        String accessToken = jwtUtil.generateAccessToken(admin.getUsername(), "admin");
        String refreshToken = jwtUtil.generateRefreshToken(admin.getUsername(), "admin");

        // 存储哈希后的 token，防止数据库泄露时明文 token 被直接利用
        String hashedAccessToken = hashToken(accessToken);
        String hashedRefreshToken = hashToken(refreshToken);
        admin.setAccessToken(hashedAccessToken);
        admin.setRefreshToken(hashedRefreshToken);
        admin.setLastLogin(LocalDateTime.now());
        adminRepository.save(admin);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", admin.getId());
        userInfo.put("username", admin.getUsername());
        userInfo.put("role", "admin");
        userInfo.put("email", admin.getEmail());
        userInfo.put("createTime", admin.getCreateTime());

        return LoginResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .userInfo(userInfo)
                .build();
    }

    @Transactional
    public LoginResponse userLogin(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));

        // Check account lockout
        if (user.getFailedLoginAttempts() != null && user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS
                && user.getLockTime() != null
                && user.getLockTime().plusMinutes(LOCKOUT_DURATION_MINUTES).isAfter(LocalDateTime.now())) {
            throw new BusinessException("账户已锁定，请" + LOCKOUT_DURATION_MINUTES + "分钟后重试");
        }
        // If lock has expired, reset attempts
        if (user.getFailedLoginAttempts() != null && user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS
                && user.getLockTime() != null
                && user.getLockTime().plusMinutes(LOCKOUT_DURATION_MINUTES).isBefore(LocalDateTime.now())) {
            user.setFailedLoginAttempts(0);
            user.setLockTime(null);
        }

        if (!passwordUtil.matches(request.getPassword(), user.getPassword())) {
            // Record failed attempt
            int attempts = (user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts()) + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setLockTime(LocalDateTime.now());
            }
            userRepository.save(user);
            throw new BusinessException("用户名或密码错误");
        }

        // Reset failed attempts on successful login
        if (user.getFailedLoginAttempts() != null && user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
            user.setLockTime(null);
        }

        if (!Boolean.TRUE.equals(user.getStatus())) {
            throw new BusinessException("账号已被禁用");
        }

        String accessToken = jwtUtil.generateAccessToken(user.getUsername(), "user");
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername(), "user");

        // 存储哈希后的 token，防止数据库泄露时明文 token 被直接利用
        String hashedAccessToken = hashToken(accessToken);
        String hashedRefreshToken = hashToken(refreshToken);
        user.setAccessToken(hashedAccessToken);
        user.setRefreshToken(hashedRefreshToken);
        user.setLastLoginTime(LocalDateTime.now());
        user.setLoginCount((user.getLoginCount() != null ? user.getLoginCount() : 0) + 1);
        userRepository.save(user);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("role", "user");
        userInfo.put("email", user.getEmail());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("createTime", user.getCreateTime());

        return LoginResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .userInfo(userInfo)
                .build();
    }

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("注册信息有误，请检查后重试");
        }
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("注册信息有误，请检查后重试");
        }

        // Verify email code
        VerificationCode vCode = verificationCodeRepository
                .findTopByEmailAndTypeOrderByCreateTimeDesc(request.getEmail(), "register")
                .orElseThrow(() -> new BusinessException("验证码不存在"));

        if (vCode.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("验证码已过期");
        }
        // 限制验证码尝试次数，防止暴力破解
        if (vCode.getAttempts() != null && vCode.getAttempts() >= MAX_FAILED_ATTEMPTS) {
            verificationCodeRepository.delete(vCode);
            throw new BusinessException("验证码已失效，请重新获取");
        }
        vCode.setAttempts((vCode.getAttempts() == null ? 0 : vCode.getAttempts()) + 1);
        verificationCodeRepository.save(vCode);
        if (!constantTimeEquals(vCode.getCode(), request.getCode())) {
            throw new BusinessException("验证码错误");
        }
        // 删除已使用的验证码，防止重复使用
        verificationCodeRepository.delete(vCode);

        // 密码强度校验
        validatePasswordStrength(request.getPassword());

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordUtil.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public void sendEmailCode(String email, String type) {
        // 60秒内不能重复发送
        boolean recentExists = verificationCodeRepository.existsRecentCode(
                email, type, LocalDateTime.now().minusSeconds(CODE_SEND_COOLDOWN_SECONDS));
        if (recentExists) {
            throw new BusinessException("请60秒后再试");
        }

        // Generate 6-digit code
        String code = String.format("%06d", new SecureRandom().nextInt(1000000));

        VerificationCode vCode = new VerificationCode();
        vCode.setEmail(email);
        vCode.setCode(code);
        vCode.setType(type);
        vCode.setExpireTime(LocalDateTime.now().plusMinutes(CODE_EXPIRE_MINUTES));
        verificationCodeRepository.save(vCode);

        try {
            emailService.sendVerificationCodeSync(email, code, type);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", email, e.getMessage());
            verificationCodeRepository.delete(vCode);
            throw new BusinessException("验证码发送失败，请稍后重试");
        }
    }

    @Transactional
    public LoginResponse refreshToken(TokenRefreshRequest request) {
        String refreshToken = request.getRefreshToken();
        if (!jwtUtil.isTokenValid(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            throw new BusinessException("Refresh token 无效或已过期");
        }

        String username = jwtUtil.extractUsername(refreshToken);
        String role = jwtUtil.extractRole(refreshToken);

        // 验证 refresh token 是否与数据库中存储的哈希值一致，防止已失效的 token 被重复使用
        // 复用查询结果，避免后续再查一次数据库
        String hashedRefreshToken = hashToken(refreshToken);
        if ("admin".equals(role)) {
            Admin admin = adminRepository.findByUsername(username)
                    .orElseThrow(() -> new BusinessException("Refresh token 已失效，请重新登录"));
            if (admin.getRefreshToken() == null || !admin.getRefreshToken().equals(hashedRefreshToken)) {
                throw new BusinessException("Refresh token 已失效，请重新登录");
            }

            String newAccessToken = jwtUtil.generateAccessToken(username, role);
            String newRefreshToken = jwtUtil.generateRefreshToken(username, role);

            admin.setAccessToken(hashToken(newAccessToken));
            admin.setRefreshToken(hashToken(newRefreshToken));
            adminRepository.save(admin);

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("username", username);
            userInfo.put("role", role);

            return LoginResponse.builder()
                    .token(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .userInfo(userInfo)
                    .build();
        } else {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new BusinessException("Refresh token 已失效，请重新登录"));
            if (user.getRefreshToken() == null || !user.getRefreshToken().equals(hashedRefreshToken)) {
                throw new BusinessException("Refresh token 已失效，请重新登录");
            }

            String newAccessToken = jwtUtil.generateAccessToken(username, role);
            String newRefreshToken = jwtUtil.generateRefreshToken(username, role);

            user.setAccessToken(hashToken(newAccessToken));
            user.setRefreshToken(hashToken(newRefreshToken));
            userRepository.save(user);

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("username", username);
            userInfo.put("role", role);

            return LoginResponse.builder()
                    .token(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .userInfo(userInfo)
                    .build();
        }
    }

    private Map<String, Object> generateBindToken(Integer userId) {
        String token = UUID.randomUUID().toString();

        BindToken bindToken = new BindToken();
        bindToken.setToken(token);
        bindToken.setUserId(userId);
        bindToken.setExpireTime(LocalDateTime.now().plusMinutes(CODE_EXPIRE_MINUTES));
        bindToken.setUsed(false);
        bindTokenRepository.save(bindToken);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        return result;
    }

    public Map<String, Object> getBindToken(Integer userId) {
        return generateBindToken(userId);
    }

    @Transactional
    public boolean validateBindToken(Integer userId, String token) {
        if (token == null || token.isEmpty()) return false;

        Optional<BindToken> optBindToken = bindTokenRepository.findByTokenAndUsedFalse(token);
        if (optBindToken.isEmpty()) return false;

        BindToken bindToken = optBindToken.get();

        // 检查是否过期
        if (bindToken.getExpireTime().isBefore(LocalDateTime.now())) {
            bindTokenRepository.delete(bindToken);
            return false;
        }

        // 验证 token 归属：只有生成该 token 的用户才能使用
        if (bindToken.getUserId() != null && userId != null
                && !bindToken.getUserId().equals(userId)) {
            return false;
        }

        // 标记为已使用（一次性令牌）
        bindToken.setUsed(true);
        bindTokenRepository.save(bindToken);
        return true;
    }

    /**
     * 定时清理过期的绑定令牌，每小时执行一次
     */
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void cleanupExpiredBindTokens() {
        try {
            bindTokenRepository.deleteByExpireTimeBefore(LocalDateTime.now());
        } catch (Exception e) {
            log.warn("清理过期绑定令牌失败: {}", e.getMessage());
        }
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        VerificationCode vCode = verificationCodeRepository
                .findTopByEmailAndTypeOrderByCreateTimeDesc(request.getEmail(), "reset")
                .orElseThrow(() -> new BusinessException("验证码不存在"));

        if (vCode.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("验证码已过期");
        }
        // 限制验证码尝试次数，防止暴力破解
        if (vCode.getAttempts() != null && vCode.getAttempts() >= MAX_FAILED_ATTEMPTS) {
            verificationCodeRepository.delete(vCode);
            throw new BusinessException("验证码已失效，请重新获取");
        }
        vCode.setAttempts((vCode.getAttempts() == null ? 0 : vCode.getAttempts()) + 1);
        verificationCodeRepository.save(vCode);
        if (!constantTimeEquals(vCode.getCode(), request.getCode())) {
            throw new BusinessException("验证码错误");
        }
        // 删除已使用的验证码，防止重复使用
        verificationCodeRepository.delete(vCode);

        // 密码强度校验
        validatePasswordStrength(request.getNewPassword());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("用户不存在"));

        user.setPassword(passwordUtil.encode(request.getNewPassword()));
        user.setUpdateTime(LocalDateTime.now());
        userRepository.save(user);
    }

    /**
     * 密码强度校验：至少8位，且必须包含大写字母、小写字母和数字。
     *
     * @param password 待校验的密码
     * @throws BusinessException 密码不符合要求时抛出
     */
    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new BusinessException("密码长度不能少于8位");
        }
        if (password.length() > 50) {
            throw new BusinessException("密码长度不能超过50位");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new BusinessException("密码必须包含至少一个大写字母");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new BusinessException("密码必须包含至少一个小写字母");
        }
        if (!password.matches(".*\\d.*")) {
            throw new BusinessException("密码必须包含至少一个数字");
        }
    }

    /**
     * 常量时间字符串比较，防止时序攻击。
     * 使用 MessageDigest.isEqual 对字节数组进行比较，耗时与内容无关。
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(
                a.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                b.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * 对 JWT token 进行 SHA-256 哈希，用于数据库存储。
     * 避免数据库泄露时明文 token 直接被利用，提升会话安全性。
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Token 哈希计算失败", e);
        }
    }
}
