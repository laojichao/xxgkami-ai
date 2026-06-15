package org.xxg.backend.backend.util;

import org.springframework.stereotype.Component;
import java.util.Base64;
import java.util.UUID;

/**
 * 卡密混淆工具类
 * <p>提供卡密的生成、加密键生成和混淆/反混淆功能。</p>
 * <p>卡密格式为16位大写字母数字组合，加密键使用SHA-256哈希。</p>
 */
@Component
public class CustomCardObfuscator {

    public String generateCardKey() {
        // Generate a random card key like: 16-char alphanumeric
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return uuid.substring(0, 16).toUpperCase();
    }

    public String generateEncryptedKey(String cardKey) {
        // Simple hash-based encrypted key for lookup
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(cardKey.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("卡密哈希计算失败", e);
        }
    }

    public String obfuscateCardKey(String cardKey, String salt, String iv, String signData) {
        // Combine: salt$encrypted_data$sign
        return salt + "$" + cardKey + "$" + signData;
    }

    public String[] deobfuscateCardKey(String obfuscated) {
        String[] parts = obfuscated.split("\\$", 3);
        if (parts.length == 3) {
            return parts;
        }
        return null;
    }
}
