package org.xxg.backend.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xxg.backend.backend.dto.*;
import org.xxg.backend.backend.entity.*;
import org.xxg.backend.backend.exception.BusinessException;
import org.xxg.backend.backend.mapper.*;
import org.xxg.backend.backend.util.JwtUtil;
import org.xxg.backend.backend.util.PasswordUtil;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final VerificationCodeRepository verificationCodeRepository;
    private final JwtUtil jwtUtil;
    private final PasswordUtil passwordUtil;
    private final EmailService emailService;

    // 内存存储 bind token，生产环境建议用 Redis
    private final Map<String, LocalDateTime> bindTokens = new ConcurrentHashMap<>();

    public AuthService(AdminRepository adminRepository, UserRepository userRepository,
                       VerificationCodeRepository verificationCodeRepository,
                       JwtUtil jwtUtil, PasswordUtil passwordUtil, EmailService emailService) {
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
        this.verificationCodeRepository = verificationCodeRepository;
        this.jwtUtil = jwtUtil;
        this.passwordUtil = passwordUtil;
        this.emailService = emailService;
    }

    public LoginResponse adminLogin(LoginRequest request) {
        Admin admin = adminRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("管理员不存在"));

        if (!passwordUtil.matches(request.getPassword(), admin.getPassword())) {
            throw new BusinessException("密码错误");
        }

        String accessToken = jwtUtil.generateAccessToken(admin.getUsername(), "admin");
        String refreshToken = jwtUtil.generateRefreshToken(admin.getUsername(), "admin");

        admin.setAccessToken(accessToken);
        admin.setRefreshToken(refreshToken);
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

    public LoginResponse userLogin(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (!passwordUtil.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("密码错误");
        }

        if (!user.getStatus()) {
            throw new BusinessException("账号已被禁用");
        }

        String accessToken = jwtUtil.generateAccessToken(user.getUsername(), "user");
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername(), "user");

        user.setAccessToken(accessToken);
        user.setRefreshToken(refreshToken);
        user.setLastLoginTime(LocalDateTime.now());
        user.setLoginCount(user.getLoginCount() + 1);
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
            throw new BusinessException("用户名已存在");
        }
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("邮箱已被注册");
        }

        // Verify email code
        VerificationCode vCode = verificationCodeRepository
                .findTopByEmailAndTypeOrderByCreateTimeDesc(request.getEmail(), "register")
                .orElseThrow(() -> new BusinessException("验证码不存在"));

        if (vCode.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("验证码已过期");
        }
        if (!vCode.getCode().equals(request.getCode())) {
            throw new BusinessException("验证码错误");
        }
        // 删除已使用的验证码，防止重复使用
        verificationCodeRepository.delete(vCode);

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordUtil.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userRepository.save(user);
    }

    public void sendEmailCode(String email, String type) {
        // 60秒内不能重复发送
        boolean recentExists = verificationCodeRepository.existsRecentCode(
                email, type, LocalDateTime.now().minusSeconds(60));
        if (recentExists) {
            throw new BusinessException("请60秒后再试");
        }

        // Generate 6-digit code
        String code = String.format("%06d", new SecureRandom().nextInt(1000000));

        VerificationCode vCode = new VerificationCode();
        vCode.setEmail(email);
        vCode.setCode(code);
        vCode.setType(type);
        vCode.setExpireTime(LocalDateTime.now().plusMinutes(10));
        verificationCodeRepository.save(vCode);

        try {
            emailService.sendVerificationCode(email, code, type);
        } catch (Exception e) {
            // Log error but don't fail - code is saved in DB
            log.error("Failed to send email to {}: {}", email, e.getMessage());
        }
    }

    public LoginResponse refreshToken(TokenRefreshRequest request) {
        String refreshToken = request.getRefreshToken();
        if (!jwtUtil.isTokenValid(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            throw new BusinessException("Refresh token 无效或已过期");
        }

        String username = jwtUtil.extractUsername(refreshToken);
        String role = jwtUtil.extractRole(refreshToken);

        String newAccessToken = jwtUtil.generateAccessToken(username, role);
        String newRefreshToken = jwtUtil.generateRefreshToken(username, role);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("username", username);
        userInfo.put("role", role);

        return LoginResponse.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .userInfo(userInfo)
                .build();
    }

    private Map<String, Object> generateBindToken() {
        String token = UUID.randomUUID().toString();
        bindTokens.put(token, LocalDateTime.now().plusMinutes(10));
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        return result;
    }

    public Map<String, Object> getBindToken() {
        return generateBindToken();
    }

    public boolean validateBindToken(Integer userId, String token) {
        if (token == null || token.isEmpty()) return false;
        LocalDateTime expireTime = bindTokens.get(token);
        if (expireTime == null) return false;
        if (expireTime.isBefore(LocalDateTime.now())) {
            bindTokens.remove(token);
            return false;
        }
        bindTokens.remove(token); // 一次性使用
        return true;
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        VerificationCode vCode = verificationCodeRepository
                .findTopByEmailAndTypeOrderByCreateTimeDesc(request.getEmail(), "reset")
                .orElseThrow(() -> new BusinessException("验证码不存在"));

        if (vCode.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("验证码已过期");
        }
        if (!vCode.getCode().equals(request.getCode())) {
            throw new BusinessException("验证码错误");
        }
        // 删除已使用的验证码，防止重复使用
        verificationCodeRepository.delete(vCode);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("用户不存在"));

        user.setPassword(passwordUtil.encode(request.getNewPassword()));
        user.setUpdateTime(LocalDateTime.now());
        userRepository.save(user);
    }
}
