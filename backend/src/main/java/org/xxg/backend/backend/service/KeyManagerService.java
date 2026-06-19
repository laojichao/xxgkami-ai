package org.xxg.backend.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xxg.backend.backend.util.AdvancedCryptoUtil;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * ECC密钥管理服务
 * 负责ECC密钥对的生成、存储和读取，用于数据加密和签名验证。
 * <p>私钥采用 AES-256-GCM 加密后存储到磁盘（.enc 文件），加密密钥从环境变量
 * {@code KEY_ENCRYPTION_KEY}（Base64 编码的 256 位密钥）获取。
 * <p><b>安全要求：</b>必须配置环境变量 {@code KEY_ENCRYPTION_KEY}，否则服务启动时将抛出异常拒绝启动。
 * 这样保证加密密钥可持久化，避免重启后无法解密私钥。</p>
 */
@Service
public class KeyManagerService {

    private static final Logger log = LoggerFactory.getLogger(KeyManagerService.class);

    private final AdvancedCryptoUtil cryptoUtil;

    /** 密钥文件存储目录 */
    private static final String KEY_DIR = "keys";
    /** 加密私钥文件扩展名 */
    private static final String ENC_EXTENSION = ".enc";
    /** GCM 认证标签长度（比特） */
    private static final int GCM_TAG_LENGTH = 128;
    /** GCM 初始向量长度（字节） */
    private static final int GCM_IV_LENGTH = 12;

    /** 缓存从环境变量读取的加密密钥（Base64），启动时校验一次 */
    private final String cachedEncryptionKey;

    public KeyManagerService(AdvancedCryptoUtil cryptoUtil) {
        this.cryptoUtil = cryptoUtil;
        // 安全修复：启动时强制校验 KEY_ENCRYPTION_KEY 环境变量，未配置则拒绝启动
        String envKey = System.getenv("KEY_ENCRYPTION_KEY");
        if (envKey == null || envKey.isBlank()) {
            throw new IllegalStateException(
                    "============================================================\n"
                    + "安全配置缺失：必须配置环境变量 KEY_ENCRYPTION_KEY\n"
                    + "该变量为 Base64 编码的 256 位 AES 密钥，用于加密 ECC 私钥。\n"
                    + "未配置将导致重启后无法解密私钥，存在密钥丢失风险。\n"
                    + "生成方法：使用任意编程语言生成 32 字节随机数并 Base64 编码。\n"
                    + "============================================================");
        }
        this.cachedEncryptionKey = envKey.trim();
        log.info("已从环境变量 KEY_ENCRYPTION_KEY 加载私钥加密密钥");
    }

    /**
     * 生成ECC密钥对并保存到文件。
     * <p>公钥以明文存储（public.key），私钥以 AES-GCM 加密后存储（private.key.enc）。</p>
     *
     * @return 格式为"公钥|私钥"的密钥对字符串（内存中返回明文，磁盘上私钥已加密）
     * @throws Exception 密钥生成或文件写入异常
     */
    public String generateAndSaveKeys() throws Exception {
        new File(KEY_DIR).mkdirs();
        String keyPair = cryptoUtil.generateEccKeyPair();
        String[] parts = keyPair.split("\\|");

        // 公钥以明文存储
        Files.writeString(Paths.get(KEY_DIR, "public.key"), parts[0]);

        // 私钥加密后存储
        String encryptionKey = getEncryptionKey();
        String encryptedPrivateKey = encryptKey(parts[1], encryptionKey);
        Files.writeString(Paths.get(KEY_DIR, "private.key" + ENC_EXTENSION), encryptedPrivateKey);

        log.info("ECC 密钥对已生成并保存，私钥已使用 AES-256-GCM 加密存储");
        return keyPair;
    }

    /**
     * 读取公钥
     * @return 公钥字符串，文件不存在返回null
     * @throws Exception 文件读取异常
     */
    public String getPublicKey() throws Exception {
        Path path = Paths.get(KEY_DIR, "public.key");
        return Files.exists(path) ? Files.readString(path) : null;
    }

    /**
     * 读取私钥（自动解密）
     * <p>优先读取加密文件 private.key.enc 并解密；
     * 若加密文件不存在但存在明文文件 private.key，则读取明文并向后兼容（同时输出警告）。</p>
     *
     * @return 私钥明文字符串，文件不存在返回null
     * @throws Exception 文件读取或解密异常
     */
    public String getPrivateKey() throws Exception {
        Path encPath = Paths.get(KEY_DIR, "private.key" + ENC_EXTENSION);
        Path plainPath = Paths.get(KEY_DIR, "private.key");

        if (Files.exists(encPath)) {
            // 读取加密文件并解密
            String ciphertext = Files.readString(encPath).trim();
            String encryptionKey = getEncryptionKey();
            return decryptKey(ciphertext, encryptionKey);
        }

        if (Files.exists(plainPath)) {
            // 向后兼容：明文私钥文件仍存在，输出警告
            log.warn("检测到明文私钥文件 private.key，建议重新生成密钥对以启用加密存储");
            return Files.readString(plainPath);
        }

        return null;
    }

    // ==================== AES-GCM 加密 / 解密 ====================

    /**
     * 获取用于加密私钥的 AES 密钥。
     * <p>密钥在服务启动时从环境变量 {@code KEY_ENCRYPTION_KEY} 读取并缓存，
     * 未配置时启动阶段即抛出异常，因此此处直接返回缓存值。</p>
     *
     * @return Base64 编码的 AES-256 密钥
     */
    private String getEncryptionKey() {
        return cachedEncryptionKey;
    }

    /**
     * 使用 AES-256-GCM 加密明文。
     * <p>格式：Base64( IV[12] || ciphertext || GCM_tag[16] )</p>
     *
     * @param plaintext    待加密的明文
     * @param encryptionKey Base64 编码的 AES-256 密钥
     * @return Base64 编码的密文（含 IV 前缀）
     * @throws Exception 加密异常
     */
    private String encryptKey(String plaintext, String encryptionKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
        byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        // 拼接 IV + 密文
        byte[] result = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
        return Base64.getEncoder().encodeToString(result);
    }

    /**
     * 使用 AES-256-GCM 解密密文。
     *
     * @param ciphertext    Base64 编码的密文（含 IV 前缀）
     * @param encryptionKey Base64 编码的 AES-256 密钥
     * @return 解密后的明文
     * @throws Exception 解密异常（密文被篡改时将抛出认证异常）
     */
    private String decryptKey(String ciphertext, String encryptionKey) throws Exception {
        byte[] data = Base64.getDecoder().decode(ciphertext);

        // 提取 IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(data, 0, iv, 0, GCM_IV_LENGTH);

        // 提取密文
        byte[] encrypted = new byte[data.length - GCM_IV_LENGTH];
        System.arraycopy(data, GCM_IV_LENGTH, encrypted, 0, encrypted.length);

        byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
        byte[] decrypted = cipher.doFinal(encrypted);
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}
