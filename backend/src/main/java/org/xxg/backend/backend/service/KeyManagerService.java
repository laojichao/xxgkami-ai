package org.xxg.backend.backend.service;

import org.springframework.stereotype.Service;
import org.xxg.backend.backend.util.AdvancedCryptoUtil;
import java.io.*;
import java.nio.file.*;

/**
 * ECC密钥管理服务
 * 负责ECC密钥对的生成、存储和读取，用于数据加密和签名验证
 */
@Service
public class KeyManagerService {
    private final AdvancedCryptoUtil cryptoUtil;
    /** 密钥文件存储目录 */
    private static final String KEY_DIR = "keys";

    public KeyManagerService(AdvancedCryptoUtil cryptoUtil) { this.cryptoUtil = cryptoUtil; }

    /**
     * 生成ECC密钥对并保存到文件
     * @return 格式为"公钥|私钥"的密钥对字符串
     * @throws Exception 密钥生成或文件写入异常
     */
    public String generateAndSaveKeys() throws Exception {
        new File(KEY_DIR).mkdirs();
        String keyPair = cryptoUtil.generateEccKeyPair();
        String[] parts = keyPair.split("\\|");
        Files.writeString(Paths.get(KEY_DIR, "public.key"), parts[0]);
        Files.writeString(Paths.get(KEY_DIR, "private.key"), parts[1]);
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
     * 读取私钥
     * @return 私钥字符串，文件不存在返回null
     * @throws Exception 文件读取异常
     */
    public String getPrivateKey() throws Exception {
        Path path = Paths.get(KEY_DIR, "private.key");
        return Files.exists(path) ? Files.readString(path) : null;
    }
}
