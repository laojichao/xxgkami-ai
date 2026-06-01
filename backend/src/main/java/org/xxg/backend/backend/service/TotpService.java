package org.xxg.backend.backend.service;

import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
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
     * 根据密钥生成当前时间窗口的TOTP验证码
     * 使用HmacSHA1算法，时间窗口为30秒
     * @param secret Base64编码的密钥
     * @return 6位数字验证码
     */
    public String generateCode(String secret) {
        try {
            byte[] key = Base64.getDecoder().decode(secret);
            long time = System.currentTimeMillis() / 30000; // 30秒时间窗口
            byte[] timeBytes = ByteBuffer.allocate(8).putLong(time).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(timeBytes);
            int offset = hash[hash.length - 1] & 0x0F; // 取哈希最后4位作为偏移量
            int code = ((hash[offset] & 0x7F) << 24) | ((hash[offset + 1] & 0xFF) << 16) |
                       ((hash[offset + 2] & 0xFF) << 8) | (hash[offset + 3] & 0xFF);
            return String.format("%06d", code % 1000000); // 取模得到6位数字
        } catch (Exception e) {
            return "000000";
        }
    }

    /**
     * 验证TOTP验证码是否正确
     * @param secret Base64编码的密钥
     * @param code 用户输入的验证码
     * @return 验证码正确返回true，否则返回false
     */
    public boolean verifyCode(String secret, String code) {
        return generateCode(secret).equals(code);
    }
}
