package org.xxg.backend.backend.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 密码加密工具类
 * 基于BCrypt算法对密码进行加密和验证，每次加密生成不同的盐值，
 * 即使相同密码也会产生不同的哈希结果，提高安全性。
 */
@Component
public class PasswordUtil {

    /** BCrypt密码编码器，自动处理盐值生成 */
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * 对明文密码进行BCrypt加密
     * @param rawPassword 明文密码
     * @return 加密后的密码哈希字符串
     */
    public String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    /**
     * 验证明文密码是否与加密后的密码匹配
     * @param rawPassword 用户输入的明文密码
     * @param encodedPassword 数据库中存储的加密密码
     * @return 匹配返回true，否则返回false
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
}
