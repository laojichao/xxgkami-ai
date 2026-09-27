package org.xxg.backend.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.xxg.backend.backend.dto.LoginRequest;
import org.xxg.backend.backend.dto.LoginResponse;
import org.xxg.backend.backend.dto.RegisterRequest;
import org.xxg.backend.backend.entity.Admin;
import org.xxg.backend.backend.entity.User;
import org.xxg.backend.backend.entity.VerificationCode;
import org.xxg.backend.backend.exception.BusinessException;
import org.xxg.backend.backend.mapper.*;
import org.xxg.backend.backend.util.JwtUtil;
import org.xxg.backend.backend.util.PasswordUtil;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * AuthService 单元测试
 * 测试登录、注册、Token 刷新等认证业务逻辑
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AdminRepository adminRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private VerificationCodeRepository verificationCodeRepository;
    @Mock
    private BindTokenRepository bindTokenRepository;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private PasswordUtil passwordUtil;
    @Mock
    private EmailService emailService;
    @Mock
    private TotpService totpService;
    @Mock
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private Admin testAdmin;

    @BeforeEach
    void setUp() {
        // 登录失败计数通过 TransactionTemplate 在独立事务中提交。
        // Mock 事务管理器使其直接执行回调，便于验证失败计数逻辑。
        lenient().when(transactionManager.getTransaction(any()))
                .thenReturn(new org.springframework.transaction.support.SimpleTransactionStatus());

        // 准备测试用户
        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("testuser");
        testUser.setPassword("$2a$10$encodedPassword");
        testUser.setEmail("test@example.com");
        testUser.setStatus(true);
        testUser.setFailedLoginAttempts(0);
        testUser.setLoginCount(0);

        // 准备测试管理员
        testAdmin = new Admin();
        testAdmin.setId(1);
        testAdmin.setUsername("admin");
        testAdmin.setPassword("$2a$10$encodedPassword");
        testAdmin.setEmail("admin@example.com");
        testAdmin.setFailedLoginAttempts(0);
    }

    @Test
    @DisplayName("用户登录成功 - 返回 Token 和用户信息")
    void userLogin_Success_ReturnsTokenAndUserInfo() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("TestPass123");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordUtil.matches("TestPass123", "$2a$10$encodedPassword")).thenReturn(true);
        when(jwtUtil.generateAccessToken("testuser", "user")).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken("testuser", "user")).thenReturn("refresh-token");

        LoginResponse response = authService.userLogin(request);

        assertNotNull(response);
        assertEquals("access-token", response.getToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertNotNull(response.getUserInfo());
        assertEquals("testuser", response.getUserInfo().get("username"));
        assertEquals("user", response.getUserInfo().get("role"));
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("用户登录失败 - 密码错误抛出异常")
    void userLogin_WrongPassword_ThrowsException() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("WrongPass");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordUtil.matches("WrongPass", "$2a$10$encodedPassword")).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.userLogin(request));
        assertEquals("用户名或密码错误", exception.getMessage());
        verify(userRepository).save(any(User.class)); // 保存失败次数
        // 失败计数必须实际递增（回归保护：计数写入曾被外层事务回滚，
        // 导致失败次数永远为 0、账户锁定机制完全失效）
        assertEquals(1, testUser.getFailedLoginAttempts());
    }

    @Test
    @DisplayName("登录失败累计达到阈值 - 账户被锁定")
    void userLogin_ReachingMaxAttempts_LocksAccount() {
        // 已有 4 次失败，本次失败后应达到阈值 5 并记录锁定时间
        testUser.setFailedLoginAttempts(4);
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("WrongPass");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordUtil.matches("WrongPass", "$2a$10$encodedPassword")).thenReturn(false);

        assertThrows(BusinessException.class, () -> authService.userLogin(request));

        assertEquals(5, testUser.getFailedLoginAttempts());
        assertNotNull(testUser.getLockTime()); // 达到阈值必须写入锁定时间
    }

    @Test
    @DisplayName("用户登录失败 - 用户不存在抛出异常")
    void userLogin_UserNotFound_ThrowsException() {
        LoginRequest request = new LoginRequest();
        request.setUsername("nonexistent");
        request.setPassword("TestPass123");

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.userLogin(request));
        assertEquals("用户名或密码错误", exception.getMessage());
    }

    @Test
    @DisplayName("用户登录失败 - 账号被禁用抛出异常")
    void userLogin_AccountDisabled_ThrowsException() {
        testUser.setStatus(false);
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("TestPass123");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordUtil.matches("TestPass123", "$2a$10$encodedPassword")).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.userLogin(request));
        assertEquals("账号已被禁用", exception.getMessage());
    }

    @Test
    @DisplayName("用户登录失败 - 账号被锁定抛出异常")
    void userLogin_AccountLocked_ThrowsException() {
        testUser.setFailedLoginAttempts(5);
        testUser.setLockTime(LocalDateTime.now().minusMinutes(5)); // 5 分钟前锁定，还在 15 分钟锁定期
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("TestPass123");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.userLogin(request));
        assertEquals("账户已锁定，请15分钟后重试", exception.getMessage());
    }

    @Test
    @DisplayName("管理员登录成功")
    void adminLogin_Success_ReturnsToken() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("AdminPass123");

        when(adminRepository.findByUsername("admin")).thenReturn(Optional.of(testAdmin));
        when(passwordUtil.matches("AdminPass123", "$2a$10$encodedPassword")).thenReturn(true);
        when(jwtUtil.generateAccessToken("admin", "admin")).thenReturn("admin-access-token");
        when(jwtUtil.generateRefreshToken("admin", "admin")).thenReturn("admin-refresh-token");

        LoginResponse response = authService.adminLogin(request);

        assertNotNull(response);
        assertEquals("admin-access-token", response.getToken());
        assertEquals("admin", response.getUserInfo().get("role"));
    }

    @Test
    @DisplayName("注册成功 - 验证码正确")
    void register_Success_WithValidCode() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("NewPass123");
        request.setEmail("new@example.com");
        request.setCode("123456");

        VerificationCode vCode = new VerificationCode();
        vCode.setCode("123456");
        vCode.setExpireTime(LocalDateTime.now().plusMinutes(5));
        vCode.setAttempts(0);

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(verificationCodeRepository.findTopByEmailAndTypeOrderByCreateTimeDesc("new@example.com", "register"))
                .thenReturn(Optional.of(vCode));
        when(passwordUtil.encode("NewPass123")).thenReturn("$2a$10$encodedNewPassword");

        assertDoesNotThrow(() -> authService.register(request));
        verify(userRepository).save(any(User.class));
        verify(verificationCodeRepository).delete(vCode); // 验证码应被删除
    }

    @Test
    @DisplayName("注册失败 - 用户名已存在")
    void register_UsernameExists_ThrowsException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existinguser");
        request.setPassword("NewPass123");
        request.setEmail("new@example.com");
        request.setCode("123456");

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.register(request));
        assertEquals("注册信息有误，请检查后重试", exception.getMessage());
    }

    @Test
    @DisplayName("注册失败 - 验证码过期")
    void register_CodeExpired_ThrowsException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("NewPass123");
        request.setEmail("new@example.com");
        request.setCode("123456");

        VerificationCode vCode = new VerificationCode();
        vCode.setCode("123456");
        vCode.setExpireTime(LocalDateTime.now().minusMinutes(1)); // 已过期
        vCode.setAttempts(0);

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(verificationCodeRepository.findTopByEmailAndTypeOrderByCreateTimeDesc("new@example.com", "register"))
                .thenReturn(Optional.of(vCode));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.register(request));
        assertEquals("验证码已过期", exception.getMessage());
    }

    @Test
    @DisplayName("注册失败 - 验证码错误")
    void register_WrongCode_ThrowsException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("NewPass123");
        request.setEmail("new@example.com");
        request.setCode("999999");

        VerificationCode vCode = new VerificationCode();
        vCode.setCode("123456");
        vCode.setExpireTime(LocalDateTime.now().plusMinutes(5));
        vCode.setAttempts(0);

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(verificationCodeRepository.findTopByEmailAndTypeOrderByCreateTimeDesc("new@example.com", "register"))
                .thenReturn(Optional.of(vCode));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.register(request));
        assertEquals("验证码错误", exception.getMessage());
    }

    @Test
    @DisplayName("注册失败 - 密码强度不足（缺少大写字母）")
    void register_WeakPassword_NoUppercase_ThrowsException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("weakpass123");
        request.setEmail("new@example.com");
        request.setCode("123456");

        VerificationCode vCode = new VerificationCode();
        vCode.setCode("123456");
        vCode.setExpireTime(LocalDateTime.now().plusMinutes(5));
        vCode.setAttempts(0);

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(verificationCodeRepository.findTopByEmailAndTypeOrderByCreateTimeDesc("new@example.com", "register"))
                .thenReturn(Optional.of(vCode));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.register(request));
        assertEquals("密码必须包含至少一个大写字母", exception.getMessage());
    }

    @Test
    @DisplayName("注册失败 - 密码太短")
    void register_WeakPassword_TooShort_ThrowsException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("Ab1");
        request.setEmail("new@example.com");
        request.setCode("123456");

        VerificationCode vCode = new VerificationCode();
        vCode.setCode("123456");
        vCode.setExpireTime(LocalDateTime.now().plusMinutes(5));
        vCode.setAttempts(0);

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(verificationCodeRepository.findTopByEmailAndTypeOrderByCreateTimeDesc("new@example.com", "register"))
                .thenReturn(Optional.of(vCode));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.register(request));
        assertEquals("密码长度不能少于8位", exception.getMessage());
    }

    @Test
    @DisplayName("登录计数器 - 首次登录 loginCount 从 null 安全递增")
    void userLogin_NullLoginCount_HandledSafely() {
        testUser.setLoginCount(null); // 模拟数据库中 loginCount 为 null
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("TestPass123");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordUtil.matches("TestPass123", "$2a$10$encodedPassword")).thenReturn(true);
        when(jwtUtil.generateAccessToken("testuser", "user")).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken("testuser", "user")).thenReturn("refresh-token");

        assertDoesNotThrow(() -> authService.userLogin(request));
        assertEquals(1, testUser.getLoginCount());
    }

    @Test
    @DisplayName("绑定令牌校验 - 令牌无归属用户时拒绝（fail-closed，防止任意用户消费）")
    void validateBindToken_NullOwner_Rejected() {
        org.xxg.backend.backend.entity.BindToken token = new org.xxg.backend.backend.entity.BindToken();
        token.setToken("orphan-token");
        token.setUserId(null); // 历史缺陷：无归属令牌曾被任意用户接受
        token.setExpireTime(LocalDateTime.now().plusMinutes(5));
        token.setUsed(false);

        when(bindTokenRepository.findByTokenAndUsedFalse("orphan-token")).thenReturn(Optional.of(token));

        assertFalse(authService.validateBindToken(1, "orphan-token"));
        // 未被消费，仍保持未使用状态
        assertFalse(Boolean.TRUE.equals(token.getUsed()));
    }

    @Test
    @DisplayName("绑定令牌校验 - 调用方未提供 userId 时拒绝")
    void validateBindToken_NullCallerId_Rejected() {
        org.xxg.backend.backend.entity.BindToken token = new org.xxg.backend.backend.entity.BindToken();
        token.setToken("owned-token");
        token.setUserId(1);
        token.setExpireTime(LocalDateTime.now().plusMinutes(5));
        token.setUsed(false);

        when(bindTokenRepository.findByTokenAndUsedFalse("owned-token")).thenReturn(Optional.of(token));

        assertFalse(authService.validateBindToken(null, "owned-token"));
    }

    @Test
    @DisplayName("绑定令牌校验 - 归属用户不匹配时拒绝")
    void validateBindToken_WrongOwner_Rejected() {
        org.xxg.backend.backend.entity.BindToken token = new org.xxg.backend.backend.entity.BindToken();
        token.setToken("owned-token");
        token.setUserId(1);
        token.setExpireTime(LocalDateTime.now().plusMinutes(5));
        token.setUsed(false);

        when(bindTokenRepository.findByTokenAndUsedFalse("owned-token")).thenReturn(Optional.of(token));

        assertFalse(authService.validateBindToken(2, "owned-token"));
    }

    @Test
    @DisplayName("绑定令牌校验 - 归属匹配时通过并标记为已使用")
    void validateBindToken_MatchingOwner_Accepted() {
        org.xxg.backend.backend.entity.BindToken token = new org.xxg.backend.backend.entity.BindToken();
        token.setToken("owned-token");
        token.setUserId(1);
        token.setExpireTime(LocalDateTime.now().plusMinutes(5));
        token.setUsed(false);

        when(bindTokenRepository.findByTokenAndUsedFalse("owned-token")).thenReturn(Optional.of(token));

        assertTrue(authService.validateBindToken(1, "owned-token"));
        assertTrue(Boolean.TRUE.equals(token.getUsed())); // 一次性令牌应被消费
    }

    @Test
    @DisplayName("生成绑定令牌 - 无归属用户时抛出异常（禁止生成孤儿令牌）")
    void getBindToken_NullUserId_ThrowsException() {
        assertThrows(BusinessException.class, () -> authService.getBindToken(null));
    }
}
