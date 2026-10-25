package org.xxg.backend.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.xxg.backend.backend.dto.*;
import org.xxg.backend.backend.entity.*;
import org.xxg.backend.backend.exception.BusinessException;
import org.xxg.backend.backend.mapper.*;
import org.xxg.backend.backend.util.JwtUtil;
import org.xxg.backend.backend.util.PasswordUtil;
import org.xxg.backend.backend.filter.JwtRequestFilter;

import java.nio.charset.StandardCharsets;
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
    /** 用户名字符白名单：字母/数字/下划线/连字符/中文 */
    private static final String USERNAME_PATTERN = "^[A-Za-z0-9_\\-\\u4e00-\\u9fa5]{3,50}$";

    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final VerificationCodeRepository verificationCodeRepository;
    private final BindTokenRepository bindTokenRepository;
    private final JwtUtil jwtUtil;
    private final PasswordUtil passwordUtil;
    private final EmailService emailService;
    private final TotpService totpService;
    private final JwtRequestFilter jwtRequestFilter;
    /** 读取站点地址配置（绑定二维码连接信息），与设置页的 site_url 配置项对应 */
    private final SettingsService settingsService;
    /** 用于在独立事务中记录登录失败次数（避免被登录失败异常回滚） */
    private final TransactionTemplate transactionTemplate;

    public AuthService(AdminRepository adminRepository, UserRepository userRepository,
                       VerificationCodeRepository verificationCodeRepository,
                       BindTokenRepository bindTokenRepository,
                       JwtUtil jwtUtil, PasswordUtil passwordUtil, EmailService emailService,
                       TotpService totpService, JwtRequestFilter jwtRequestFilter,
                       SettingsService settingsService,
                       PlatformTransactionManager transactionManager) {
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
        this.verificationCodeRepository = verificationCodeRepository;
        this.bindTokenRepository = bindTokenRepository;
        this.jwtUtil = jwtUtil;
        this.passwordUtil = passwordUtil;
        this.emailService = emailService;
        this.totpService = totpService;
        this.jwtRequestFilter = jwtRequestFilter;
        this.settingsService = settingsService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        // 必须使用 REQUIRES_NEW：TransactionTemplate 默认传播行为是 REQUIRED，
        // 会加入 adminLogin/userLogin 已有的外层事务，登录失败时抛出的异常
        // 依然会把失败计数一起回滚，锁定机制失效。REQUIRES_NEW 挂起外层事务、
        // 独立提交计数更新，不受外层回滚影响。
        this.transactionTemplate.setPropagationBehavior(
                org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional
    public LoginResponse adminLogin(LoginRequest request) {
        Admin admin = adminRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));

        checkAndResetLockout(admin.getFailedLoginAttempts(), admin.getLockTime(),
                (attempts, lockTime) -> { admin.setFailedLoginAttempts(attempts); admin.setLockTime(lockTime); },
                () -> adminRepository.save(admin));

        if (!passwordUtil.matches(request.getPassword(), admin.getPassword())) {
            // 安全修复：失败计数必须在独立事务中提交。
            // 本方法由 @Transactional 包裹，随后抛出的 BusinessException 会回滚整个事务，
            // 导致失败次数永远不会累加、账户锁定机制完全失效（可无限次暴力破解）。
            recordLoginFailureInNewTransaction(admin, "admin");
            throw new BusinessException("用户名或密码错误");
        }

        resetLoginFailures(admin.getFailedLoginAttempts(), admin.getLockTime(),
                (attempts, lockTime) -> { admin.setFailedLoginAttempts(attempts); admin.setLockTime(lockTime); });

        // TOTP 二次验证
        if (Boolean.TRUE.equals(admin.getTotpEnabled())) {
            verifyTotpCode(admin.getTotpSecret(), request.getTotpCode());
        }

        // 生成并存储 Token
        String accessToken = jwtUtil.generateAccessToken(admin.getUsername(), "admin");
        String refreshToken = jwtUtil.generateRefreshToken(admin.getUsername(), "admin");
        storeHashedTokens(accessToken, refreshToken,
                t -> admin.setAccessToken(t), t -> admin.setRefreshToken(t),
                () -> { admin.setLastLogin(LocalDateTime.now()); adminRepository.save(admin); });

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", admin.getId());
        userInfo.put("username", admin.getUsername());
        userInfo.put("role", "admin");
        userInfo.put("email", admin.getEmail());
        userInfo.put("createTime", admin.getCreateTime());
        // 前端 TOTP 管理入口依赖此字段判断"配置"还是"禁用"状态
        userInfo.put("totpEnabled", Boolean.TRUE.equals(admin.getTotpEnabled()));

        return LoginResponse.builder().token(accessToken).refreshToken(refreshToken).userInfo(userInfo).build();
    }

    @Transactional
    public LoginResponse userLogin(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));

        checkAndResetLockout(user.getFailedLoginAttempts(), user.getLockTime(),
                (attempts, lockTime) -> { user.setFailedLoginAttempts(attempts); user.setLockTime(lockTime); },
                () -> userRepository.save(user));

        if (!passwordUtil.matches(request.getPassword(), user.getPassword())) {
            // 安全修复：同 adminLogin，失败计数需独立事务提交，否则抛出异常时被回滚
            recordLoginFailureInNewTransaction(user, "user");
            throw new BusinessException("用户名或密码错误");
        }

        resetLoginFailures(user.getFailedLoginAttempts(), user.getLockTime(),
                (attempts, lockTime) -> { user.setFailedLoginAttempts(attempts); user.setLockTime(lockTime); });

        if (!Boolean.TRUE.equals(user.getStatus())) {
            throw new BusinessException("账号已被禁用");
        }

        String accessToken = jwtUtil.generateAccessToken(user.getUsername(), "user");
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername(), "user");
        storeHashedTokens(accessToken, refreshToken,
                t -> user.setAccessToken(t), t -> user.setRefreshToken(t),
                () -> {
                    user.setLastLoginTime(LocalDateTime.now());
                    user.setLoginCount((user.getLoginCount() != null ? user.getLoginCount() : 0) + 1);
                    userRepository.save(user);
                });

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("role", "user");
        userInfo.put("email", user.getEmail());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("createTime", user.getCreateTime());
        userInfo.put("totpEnabled", false);

        return LoginResponse.builder().token(accessToken).refreshToken(refreshToken).userInfo(userInfo).build();
    }

    // === 登录公共方法 ===

    /** 检查账户锁定状态，过期则自动重置 */
    private void checkAndResetLockout(Integer failedAttempts, LocalDateTime lockTime,
                                       java.util.function.BiConsumer<Integer, LocalDateTime> setter,
                                       Runnable saver) {
        if (failedAttempts != null && failedAttempts >= MAX_FAILED_ATTEMPTS && lockTime != null) {
            if (lockTime.plusMinutes(LOCKOUT_DURATION_MINUTES).isAfter(LocalDateTime.now())) {
                throw new BusinessException("账户已锁定，请" + LOCKOUT_DURATION_MINUTES + "分钟后重试");
            }
            // 锁定已过期，重置
            setter.accept(0, null);
            saver.run();
        }
    }

    /**
     * 记录登录失败并在独立事务中提交，达到阈值时锁定账户。
     * <p>必须使用独立事务：登录方法本身是 {@code @Transactional}，
     * 校验失败后抛出的 {@link BusinessException} 会回滚同一事务内的所有写操作，
     * 使失败计数无法持久化、账户锁定形同虚设。</p>
     *
     * @param account 管理员或用户实体
     * @param role    {@code "admin"} 或 {@code "user"}
     */
    private void recordLoginFailureInNewTransaction(Object account, String role) {
        transactionTemplate.executeWithoutResult(status -> {
            if ("admin".equals(role)) {
                Admin admin = (Admin) account;
                int attempts = (admin.getFailedLoginAttempts() == null ? 0 : admin.getFailedLoginAttempts()) + 1;
                admin.setFailedLoginAttempts(attempts);
                admin.setLockTime(attempts >= MAX_FAILED_ATTEMPTS ? LocalDateTime.now() : admin.getLockTime());
                adminRepository.save(admin);
            } else {
                User user = (User) account;
                int attempts = (user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts()) + 1;
                user.setFailedLoginAttempts(attempts);
                user.setLockTime(attempts >= MAX_FAILED_ATTEMPTS ? LocalDateTime.now() : user.getLockTime());
                userRepository.save(user);
            }
        });
    }

    /** 登录成功后重置失败计数 */
    private void resetLoginFailures(Integer failedAttempts, LocalDateTime lockTime,
                                     java.util.function.BiConsumer<Integer, LocalDateTime> setter) {
        if (failedAttempts != null && failedAttempts > 0) {
            setter.accept(0, null);
        }
    }

    /** 验证 TOTP 验证码 */
    private void verifyTotpCode(String encryptedSecret, String totpCode) {
        if (totpCode == null || totpCode.isBlank()) {
            throw new BusinessException("请输入 TOTP 验证码");
        }
        String decryptedSecret = totpService.decryptSecret(encryptedSecret);
        if (!totpService.verifyCode(decryptedSecret, totpCode)) {
            throw new BusinessException("TOTP 验证码错误");
        }
    }

    /** 生成 Token、哈希存储、执行保存回调 */
    private void storeHashedTokens(String accessToken, String refreshToken,
                                    java.util.function.Consumer<String> setAccess,
                                    java.util.function.Consumer<String> setRefresh,
                                    Runnable saveCallback) {
        setAccess.accept(hashToken(accessToken));
        setRefresh.accept(hashToken(refreshToken));
        saveCallback.run();
    }

    @Transactional
    public void register(RegisterRequest request) {
        validateNewUsername(request.getUsername());
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("注册信息有误，请检查后重试");
        }
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("注册信息有误，请检查后重试");
        }

        // 校验并消费邮箱验证码
        verifyEmailCode(request.getEmail(), "register", request.getCode());

        // 密码强度校验
        validatePasswordStrength(request.getPassword());

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordUtil.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        // 并发同名注册时两条请求都通过上面的 existsByUsername 预检，
        // 一条成功另一条撞唯一索引——直接捕获转为与串行路径一致的友好提示，
        // 而不是 GlobalExceptionHandler 统一转出的 409 "数据冲突"
        saveUserHandlingConflicts(user);
    }

    /**
     * 保存用户并将唯一键冲突（并发注册/创建绕过 exists 预检的场景）
     * 转为与串行路径一致的友好业务提示。
     * <p>注意：捕获后不可在同一事务内继续查询，直接抛出即可
     * （冲突时事务已标记 rollback-only）。</p>
     */
    private void saveUserHandlingConflicts(User user) {
        try {
            userRepository.save(user);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new BusinessException("用户名或邮箱已被使用，请更换后重试");
        }
    }

    /**
     * 第三方 OAuth 登录后完善账号信息（注册绑定）。
     * <p>与普通注册的区别：邮箱为可选（第三方网关已完成身份验证），无需邮箱验证码；
     * 必须携带 OAuth 网关回调下发的一次性 registerToken。</p>
     *
     * @param request 注册绑定请求（username/password/email/registerToken）
     */
    @Transactional
    public void registerBind(RegisterBindRequest request) {
        // registerToken 由第三方 OAuth 网关在回调中下发给前端，后端无签发记录，
        // 无法独立验证真伪，此处仅要求非空以区分注册入口；账号安全由密码强度兜底
        if (request.getRegisterToken() == null || request.getRegisterToken().isBlank()) {
            throw new BusinessException("缺少注册令牌，请从第三方登录入口重新进入");
        }
        validateNewUsername(request.getUsername());
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("注册信息有误，请检查后重试");
        }
        String email = request.getEmail();
        if (email != null && email.isBlank()) {
            email = null;
        }
        if (email != null && userRepository.existsByEmail(email)) {
            throw new BusinessException("注册信息有误，请检查后重试");
        }

        // 密码强度校验（与普通注册一致）
        validatePasswordStrength(request.getPassword());

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordUtil.encode(request.getPassword()));
        user.setEmail(email);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        saveUserHandlingConflicts(user);
    }

    /**
     * 校验新用户名格式与保留字。
     * <p>禁止与管理员用户名相同：部分按用户名跨表查询管理员的管理端点
     * （如 /auth/totp/*）若被同名普通用户命中会造成越权，必须从注册入口杜绝碰撞。</p>
     */
    private void validateNewUsername(String username) {
        if (username == null || !username.matches(USERNAME_PATTERN)) {
            throw new BusinessException("用户名必须为 3-50 位字母、数字、下划线、连字符或中文");
        }
        if (adminRepository.existsByUsername(username)) {
            throw new BusinessException("注册信息有误，请检查后重试");
        }
    }

    /**
     * 校验邮箱验证码：过期/超限检查、独立事务递增尝试次数、比对通过后消费。
     * <p>尝试次数递增必须在 REQUIRES_NEW 独立事务中提交——本方法的外层调用方
     * （register/resetPassword）是 @Transactional，验证码错误时抛出的异常会把
     * 同一事务内的计数更新一并回滚，导致爆破限制永远不生效（与登录失败计数
     * 曾出现的缺陷同理）。验证码以 SHA-256 哈希存储，数据库泄露不暴露在途验证码。</p>
     *
     * @param email     邮箱
     * @param type      验证码类型（register/reset/bind）
     * @param inputCode 用户输入的验证码明文
     */
    private void verifyEmailCode(String email, String type, String inputCode) {
        VerificationCode vCode = verificationCodeRepository
                .findTopByEmailAndTypeOrderByCreateTimeDesc(email, type)
                .orElseThrow(() -> new BusinessException("验证码不存在"));

        if (vCode.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("验证码已过期");
        }
        // 限制验证码尝试次数，防止暴力破解
        if (vCode.getAttempts() != null && vCode.getAttempts() >= MAX_FAILED_ATTEMPTS) {
            verificationCodeRepository.delete(vCode);
            throw new BusinessException("验证码已失效，请重新获取");
        }
        // 独立事务提交尝试次数，确保验证失败（抛异常回滚外层事务）后计数仍然生效
        transactionTemplate.executeWithoutResult(
                status -> verificationCodeRepository.incrementAttempts(vCode.getId()));
        if (!constantTimeEquals(vCode.getCode(), hashVerificationCode(inputCode))) {
            throw new BusinessException("验证码错误");
        }
        // 删除已使用的验证码，防止重复使用
        verificationCodeRepository.delete(vCode);
    }

    /** 对验证码明文做 SHA-256 哈希（十六进制），用于存储与比对 */
    private String hashVerificationCode(String code) {
        if (code == null) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(code.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("验证码哈希计算失败", e);
        }
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
        // 安全修复：验证码以 SHA-256 哈希入库，数据库泄露不暴露在途验证码
        vCode.setCode(hashVerificationCode(code));
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
            // 安全修复：刷新 token 时校验账号当前状态，防止被锁定账号继续刷新 token
            if (admin.getFailedLoginAttempts() != null && admin.getFailedLoginAttempts() >= 5
                    && admin.getLockTime() != null
                    && admin.getLockTime().plusMinutes(15).isAfter(LocalDateTime.now())) {
                throw new BusinessException("账号已被锁定，请稍后再试");
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
            // 安全修复：刷新 token 时校验账号当前状态，防止被禁用/锁定账号继续刷新 token
            if (!Boolean.TRUE.equals(user.getStatus())) {
                throw new BusinessException("账号已被禁用，请联系管理员");
            }
            if (user.getFailedLoginAttempts() != null && user.getFailedLoginAttempts() >= 5
                    && user.getLockTime() != null
                    && user.getLockTime().plusMinutes(15).isAfter(LocalDateTime.now())) {
                throw new BusinessException("账号已被锁定，请稍后再试");
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

    /**
     * 生成一次性绑定令牌（有效期 10 分钟，使用后失效）。
     *
     * @param userId 令牌归属的用户 ID，不能为空
     * @return 包含 token 的 Map
     * @throws BusinessException userId 为空时抛出（禁止生成无归属令牌）
     */
    private Map<String, Object> generateBindToken(Integer userId) {
        // 安全约束：令牌必须绑定到具体用户。
        // 生成无归属令牌（userId=null）会使 validateBindToken 的归属校验失去依据，
        // 该令牌可被任意用户消费。
        if (userId == null) {
            throw new BusinessException("无法生成绑定令牌：当前账号未关联用户");
        }
        String token = UUID.randomUUID().toString();

        BindToken bindToken = new BindToken();
        bindToken.setToken(token);
        bindToken.setUserId(userId);
        bindToken.setExpireTime(LocalDateTime.now().plusMinutes(CODE_EXPIRE_MINUTES));
        bindToken.setUsed(false);
        bindTokenRepository.save(bindToken);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        // 前端绑定二维码（UserSettingsPage）优先读取 siteUrl 构造服务器连接信息；
        // 未配置时返回空串，前端回退到当前页面地址推导
        result.put("siteUrl", settingsService.getValue("site_url", ""));
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

        // 验证 token 归属：只有生成该 token 的用户才能使用。
        // 采用 fail-closed 策略——任一侧缺失都视为不匹配。
        // 历史缺陷：条件为 `bindToken.getUserId() != null && userId != null && !equals`，
        // 只要 token 未绑定 userId（如管理员或未登录状态调用 /auth/bind/token 时生成）
        // 或调用方未传 userId，归属校验即被完全跳过，任何人都能消费该 token。
        if (bindToken.getUserId() == null || userId == null
                || !bindToken.getUserId().equals(userId)) {
            return false;
        }

        // 原子标记为已使用（一次性令牌）：条件 UPDATE 判定影响行数，
        // 消除两个并发请求同时读到 used=false、都通过校验、令牌被消费两次的 TOCTOU 窗口
        int updated = bindTokenRepository.markUsedIfUnused(bindToken.getId());
        return updated > 0;
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
        // 校验并消费验证码（尝试计数在独立事务中生效，防爆破）
        verifyEmailCode(request.getEmail(), "reset", request.getCode());

        // 密码强度校验
        validatePasswordStrength(request.getNewPassword());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("用户不存在"));

        user.setPassword(passwordUtil.encode(request.getNewPassword()));
        user.setUpdateTime(LocalDateTime.now());
        // 安全修复：重置密码后吊销全部已签发 Token。
        // 重置密码是账号被盗后的标准止损点，若旧 refresh token 仍可续签，
        // 攻击者的会话可无限延续，直到受害者本人再次登录触发轮换为止。
        user.setAccessToken(null);
        user.setRefreshToken(null);
        userRepository.save(user);
        jwtRequestFilter.invalidateAccountCache(user.getUsername(), "user");
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
