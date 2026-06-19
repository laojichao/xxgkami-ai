package org.xxg.backend.backend.util;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

/**
 * 高级加密工具类。
 * <p>提供 AES-GCM 对称加密/解密、ECC 椭圆曲线签名/验签、HMAC-SHA256 摘要签名等能力，
 * 所有密钥和密文均以 Base64 编码传输。</p>
 */
@Component
public class AdvancedCryptoUtil {

    /** AES-GCM 算法标识 */
    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    /** GCM 认证标签长度（比特） */
    private static final int GCM_TAG_LENGTH = 128;
    /** GCM 初始向量长度（字节） */
    private static final int GCM_IV_LENGTH = 12;
    /** 全局盐值，用于卡密签名场景 */
    private static final String GLOBAL_SALT = "global";

    /**
     * 生成 256 位 AES 密钥。
     *
     * @return Base64 编码的 AES 密钥
     * @throws NoSuchAlgorithmException 如果 AES 算法不可用
     */
    public String generateAesKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey key = keyGen.generateKey();
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    /**
     * 生成随机的 GCM 初始向量（IV）。
     *
     * @return Base64 编码的 IV（12 字节）
     */
    public String generateIv() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        return Base64.getEncoder().encodeToString(iv);
    }

    /**
     * 使用 AES-GCM 模式加密明文。
     *
     * @param plaintext 明文字符串
     * @param base64Key Base64 编码的 AES 密钥
     * @param base64Iv  Base64 编码的初始向量
     * @return Base64 编码的密文（含认证标签）
     * @throws Exception 加密过程中的异常
     */
    public String encrypt(String plaintext, String base64Key, String base64Iv) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        byte[] ivBytes = Base64.getDecoder().decode(base64Iv);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, ivBytes);

        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
        byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * 使用 AES-GCM 模式解密密文。
     *
     * @param base64Ciphertext Base64 编码的密文
     * @param base64Key        Base64 编码的 AES 密钥
     * @param base64Iv         Base64 编码的初始向量
     * @return 解密后的明文字符串
     * @throws Exception 解密过程中的异常（密文被篡改时会抛出认证异常）
     */
    public String decrypt(String base64Ciphertext, String base64Key, String base64Iv) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        byte[] ivBytes = Base64.getDecoder().decode(base64Iv);
        byte[] ciphertext = Base64.getDecoder().decode(base64Ciphertext);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, ivBytes);

        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
        byte[] decrypted = cipher.doFinal(ciphertext);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    /**
     * 生成 ECC（椭圆曲线）密钥对。
     *
     * @return 公钥和私钥的 Base64 编码，以 "|" 分隔（公钥|私钥）
     * @throws NoSuchAlgorithmException 如果 EC 算法不可用
     */
    public String generateEccKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        keyGen.initialize(256);
        KeyPair keyPair = keyGen.generateKeyPair();
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded())
                + "|" + Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
    }

    /**
     * 使用 ECC 私钥对数据进行数字签名（SHA256withECDSA）。
     *
     * @param data            待签名的数据
     * @param base64PrivateKey Base64 编码的 ECC 私钥（PKCS8 格式）
     * @return Base64 编码的签名值
     * @throws Exception 签名过程中的异常
     */
    public String sign(String data, String base64PrivateKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64PrivateKey);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        java.security.spec.PKCS8EncodedKeySpec keySpec = new java.security.spec.PKCS8EncodedKeySpec(keyBytes);
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initSign(privateKey);
        signature.update(data.getBytes(StandardCharsets.UTF_8));
        byte[] signed = signature.sign();
        return Base64.getEncoder().encodeToString(signed);
    }

    /**
     * 使用 ECC 公钥验证数字签名（SHA256withECDSA）。
     *
     * @param data            原始数据
     * @param base64Signature Base64 编码的签名值
     * @param base64PublicKey Base64 编码的 ECC 公钥（X509 格式）
     * @return 签名是否有效
     * @throws Exception 验签过程中的异常
     */
    public boolean verify(String data, String base64Signature, String base64PublicKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64PublicKey);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        java.security.spec.X509EncodedKeySpec keySpec = new java.security.spec.X509EncodedKeySpec(keyBytes);
        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initVerify(publicKey);
        signature.update(data.getBytes(StandardCharsets.UTF_8));
        return signature.verify(Base64.getDecoder().decode(base64Signature));
    }

    /**
     * 获取全局盐值。
     *
     * @return 全局盐值字符串
     */
    public String getGlobalSalt() {
        return GLOBAL_SALT;
    }

    /**
     * HMAC-SHA256 签名，使用 AES 密钥作为 HMAC 密钥。
     * 用于卡密签名场景（密钥为 AES 格式）。
     */
    public String hmacSign(String data, String base64AesKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64AesKey);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(keySpec);
        byte[] result = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(result);
    }

    /**
     * HMAC-SHA256 签名，使用原始字符串作为密钥（适用于 salt 等非 Base64 密钥场景）。
     * 用于卡密签名校验场景，密钥直接取字符串字节，不进行 Base64 解码。
     *
     * @param data      待签名数据
     * @param stringKey 字符串密钥（如 salt）
     * @return Base64 编码的签名值
     * @throws Exception 签名异常
     */
    public String hmacSignWithStringKey(String data, String stringKey) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(
                stringKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(keySpec);
        byte[] result = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(result);
    }

    /**
     * HMAC-SHA256 验签（常量时间比较，防止时序攻击）。
     */
    public boolean hmacVerify(String data, String base64Signature, String base64AesKey) throws Exception {
        String calculated = hmacSign(data, base64AesKey);
        return MessageDigest.isEqual(
                calculated.getBytes(StandardCharsets.UTF_8),
                base64Signature.getBytes(StandardCharsets.UTF_8));
    }
}
