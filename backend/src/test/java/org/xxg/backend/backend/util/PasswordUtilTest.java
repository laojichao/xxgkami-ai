package org.xxg.backend.backend.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PasswordUtil 单元测试
 * 测试密码哈希和验证功能
 */
class PasswordUtilTest {

    private PasswordUtil passwordUtil;

    @BeforeEach
    void setUp() {
        passwordUtil = new PasswordUtil();
    }

    @Test
    @DisplayName("BCrypt 编码后的密码应能正确验证")
    void encode_BCrypt_MatchesCorrectPassword() {
        String rawPassword = "TestPass123";
        String encoded = passwordUtil.encode(rawPassword);

        assertNotNull(encoded);
        assertTrue(passwordUtil.matches(rawPassword, encoded));
    }

    @Test
    @DisplayName("BCrypt 编码后的密码不应匹配错误密码")
    void encode_BCrypt_DoesNotMatchWrongPassword() {
        String encoded = passwordUtil.encode("TestPass123");

        assertFalse(passwordUtil.matches("WrongPass456", encoded));
    }

    @Test
    @DisplayName("同一密码多次编码应产生不同的哈希值（盐值不同）")
    void encode_SamePassword_DifferentHashes() {
        String hash1 = passwordUtil.encode("TestPass123");
        String hash2 = passwordUtil.encode("TestPass123");

        assertNotEquals(hash1, hash2);
        // 但两个哈希都应该能验证原始密码
        assertTrue(passwordUtil.matches("TestPass123", hash1));
        assertTrue(passwordUtil.matches("TestPass123", hash2));
    }

    @Test
    @DisplayName("空密码编码不应抛出异常")
    void encode_EmptyPassword_DoesNotThrow() {
        assertDoesNotThrow(() -> passwordUtil.encode(""));
    }

    @Test
    @DisplayName("包含特殊字符的密码应能正确编码和验证")
    void encode_SpecialCharacters_MatchesCorrectly() {
        String rawPassword = "P@ss!#$%^&*()_+123";
        String encoded = passwordUtil.encode(rawPassword);

        assertTrue(passwordUtil.matches(rawPassword, encoded));
    }

    @Test
    @DisplayName("Unicode 密码应能正确编码和验证")
    void encode_UnicodePassword_MatchesCorrectly() {
        String rawPassword = "密码测试123ABC";
        String encoded = passwordUtil.encode(rawPassword);

        assertTrue(passwordUtil.matches(rawPassword, encoded));
    }

    @Test
    @DisplayName("长密码应能正确编码和验证")
    void encode_LongPassword_MatchesCorrectly() {
        String rawPassword = "A".repeat(100) + "1a";
        String encoded = passwordUtil.encode(rawPassword);

        assertTrue(passwordUtil.matches(rawPassword, encoded));
    }
}
