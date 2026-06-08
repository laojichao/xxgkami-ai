package org.xxg.backend.backend.service;

import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * TOTP两步验证服务
 * 实现基于时间的一次性密码算法（RFC 6238），用于两步验证功能
 */
@Service
public class TotpService {

    /**
     * 生成TOTP密钥（20字节随机数的Base64编码）
     * @return Base64编码的密钥字符串
     */
    public String generateSecret() {
        byte[] buffer = new byte[20];
        new SecureRandom().nextBytes(buffer);
        return Base64.getEncoder().encodeToString(buffer);
    }

    /**
     * 根据密钥和时间步长生成TOTP验证码
     * 使用HmacSHA1算法，时间窗口为30秒
     * @param secret Base64编码的密钥
     * @param timeStep 时间步长（当前毫秒数 / 30000）
     * @return 6位数字验证码
     * @throws Exception 密钥解码或HMAC计算失败时抛出
     */
    private String generateCodeForTimeStep(String secret, long timeStep) throws Exception {
        byte[] key = Base64.getDecoder().decode(secret);
        byte[] timeBytes = ByteBuffer.allocate(8).putLong(timeStep).array();
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key, "HmacSHA1"));
        byte[] hash = mac.doFinal(timeBytes);
        int offset = hash[hash.length - 1] & 0x0F;
        int code = ((hash[offset] & 0x7F) << 24) | ((hash[offset + 1] & 0xFF) << 16) |
                   ((hash[offset + 2] & 0xFF) << 8) | (hash[offset + 3] & 0xFF);
        return String.format("%06d", code % 1000000);
    }

    /**
     * 根据密钥生成当前时间窗口的TOTP验证码
     * 使用HmacSHA1算法，时间窗口为30秒
     * @param secret Base64编码的密钥
     * @return 6位数字验证码
     * @throws Exception 密钥解码或HMAC计算失败时抛出
     */
    public String generateCode(String secret) throws Exception {
        long timeStep = System.currentTimeMillis() / 30000;
        return generateCodeForTimeStep(secret, timeStep);
    }

    /**
     * 验证TOTP验证码是否正确（RFC 6238兼容，±1窗口容差）
     * 检查当前时间窗口及前后各一个窗口，容忍轻微的时钟偏差
     * @param secret Base64编码的密钥
     * @param code 用户输入的验证码
     * @return 验证码在容差范围内返回true，否则返回false
     */
    public boolean verifyCode(String secret, String code) {
        if (secret == null || code == null || code.isBlank()) return false;
        try {
            long currentStep = System.currentTimeMillis() / 30000;
            // 检查当前窗口及前后各1个窗口（共3个窗口），容忍±30秒时钟偏差
            for (long step = currentStep - 1; step <= currentStep + 1; step++) {
                String expected = generateCodeForTimeStep(secret, step);
                // 使用常量时间比较，防止时序攻击
                if (MessageDigest.isEqual(
                        expected.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        code.getBytes(java.nio.charset.StandardCharsets.UTF_8))) return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
