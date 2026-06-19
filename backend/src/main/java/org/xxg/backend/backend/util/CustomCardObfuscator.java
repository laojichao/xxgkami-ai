package org.xxg.backend.backend.util;

import org.springframework.stereotype.Component;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 卡密混淆工具类
 * <p>提供卡密的生成、加密键生成和混淆/反混淆功能。</p>
 * <p>卡密格式为 20 位大小写字母+数字组合，使用 SecureRandom 生成保证高熵。
 * 加密键使用 SHA-256 哈希。</p>
 */
@Component
public class CustomCardObfuscator {

    /** 卡密字符集：大小写字母 + 数字，共 62 个字符 */
    private static final String CARD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    /** 卡密长度：20 位（熵约 119 bit，远高于原 16 位 hex 的 64 bit） */
    private static final int CARD_LENGTH = 20;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 生成卡密明文：20 位大小写字母+数字组合，使用 SecureRandom 保证高熵。
     * @return 卡密明文
     */
    public String generateCardKey() {
        StringBuilder sb = new StringBuilder(CARD_LENGTH);
        for (int i = 0; i < CARD_LENGTH; i++) {
            sb.append(CARD_CHARS.charAt(secureRandom.nextInt(CARD_CHARS.length())));
        }
        return sb.toString();
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
