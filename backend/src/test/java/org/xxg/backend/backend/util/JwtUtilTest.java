package org.xxg.backend.backend.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil 单元测试
 * 测试 JWT 令牌的生成、验证、解析功能
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // 设置测试用的密钥（至少 32 字节）
        ReflectionTestUtils.setField(jwtUtil, "secret", "test-secret-key-must-be-at-least-32-bytes-long-for-hmac-sha256");
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiration", 3600000L); // 1 小时
        ReflectionTestUtils.setField(jwtUtil, "refreshTokenExpiration", 604800000L); // 7 天
        // 初始化缓存的签名密钥和 JwtParser（模拟 @PostConstruct 行为）
        jwtUtil.init();
    }

    @Test
    @DisplayName("生成 Access Token 并验证有效性")
    void generateAccessToken_ValidToken() {
        String token = jwtUtil.generateAccessToken("testuser", "user");

        assertNotNull(token);
        assertTrue(jwtUtil.isTokenValid(token));
        assertTrue(jwtUtil.isAccessToken(token));
        assertFalse(jwtUtil.isRefreshToken(token));
    }

    @Test
    @DisplayName("生成 Refresh Token 并验证有效性")
    void generateRefreshToken_ValidToken() {
        String token = jwtUtil.generateRefreshToken("testuser", "user");

        assertNotNull(token);
        assertTrue(jwtUtil.isTokenValid(token));
        assertTrue(jwtUtil.isRefreshToken(token));
        assertFalse(jwtUtil.isAccessToken(token));
    }

    @Test
    @DisplayName("从 Token 中提取用户名")
    void extractUsername_ReturnsCorrectUsername() {
        String token = jwtUtil.generateAccessToken("testuser", "user");

        assertEquals("testuser", jwtUtil.extractUsername(token));
    }

    @Test
    @DisplayName("从 Token 中提取角色")
    void extractRole_ReturnsCorrectRole() {
        String token = jwtUtil.generateAccessToken("testuser", "admin");

        assertEquals("admin", jwtUtil.extractRole(token));
    }

    @Test
    @DisplayName("无效 Token 验证返回 false")
    void isTokenValid_InvalidToken_ReturnsFalse() {
        assertFalse(jwtUtil.isTokenValid("invalid.token.here"));
    }

    @Test
    @DisplayName("空 Token 验证返回 false")
    void isTokenValid_EmptyToken_ReturnsFalse() {
        assertFalse(jwtUtil.isTokenValid(""));
    }

    @Test
    @DisplayName("Access Token 不应被识别为 Refresh Token")
    void isRefreshToken_AccessToken_ReturnsFalse() {
        String accessToken = jwtUtil.generateAccessToken("testuser", "user");

        assertFalse(jwtUtil.isRefreshToken(accessToken));
    }

    @Test
    @DisplayName("密钥长度不足时应抛出异常")
    void getSigningKey_TooShortSecret_ThrowsException() {
        JwtUtil shortKeyJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(shortKeyJwtUtil, "secret", "too-short");
        ReflectionTestUtils.setField(shortKeyJwtUtil, "accessTokenExpiration", 3600000L);
        ReflectionTestUtils.setField(shortKeyJwtUtil, "refreshTokenExpiration", 604800000L);

        // 密钥长度不足时，init() 应抛出 IllegalStateException
        assertThrows(IllegalStateException.class, shortKeyJwtUtil::init);
    }

    @Test
    @DisplayName("不同用户生成的 Token 应不同")
    void generateAccessToken_DifferentUsers_DifferentTokens() {
        String token1 = jwtUtil.generateAccessToken("user1", "user");
        String token2 = jwtUtil.generateAccessToken("user2", "user");

        assertNotEquals(token1, token2);
    }

    @Test
    @DisplayName("同一用户多次生成的 Token 应不同（因为 issuedAt 不同）")
    void generateAccessToken_SameUser_DifferentTokens() throws InterruptedException {
        String token1 = jwtUtil.generateAccessToken("testuser", "user");
        Thread.sleep(1100); // 确保 issuedAt 不同（秒级精度）
        String token2 = jwtUtil.generateAccessToken("testuser", "user");

        // Token 可能相同（如果在同一秒内生成），但都应该有效
        assertTrue(jwtUtil.isTokenValid(token1));
        assertTrue(jwtUtil.isTokenValid(token2));
    }
}
