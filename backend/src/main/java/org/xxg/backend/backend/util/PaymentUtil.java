package org.xxg.backend.backend.util;

import org.springframework.stereotype.Component;
import java.util.*;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 支付工具类
 * <p>提供易支付接口的签名生成、签名验证和订单号生成功能。</p>
 * <p>签名算法：将参数按key排序拼接后附加密钥，进行MD5哈希并转大写。</p>
 * <p>安全说明：MD5 是易支付协议要求的签名算法，存在碰撞风险。
 * 如果支付网关支持 HMAC-SHA256，建议切换到 {@link #generateSignHmac} 方法。</p>
 */
@Component
public class PaymentUtil {

    /**
     * 生成支付签名（MD5，兼容易支付协议）
     * <p>安全说明：MD5 存在已知碰撞攻击，但这是易支付协议的强制要求。
     * 如果支付网关支持更安全的算法，请使用 {@link #generateSignHmac}。</p>
     */
    public String generateSign(Map<String, String> params, String key) {
        String signContent = buildSignContent(params);
        return md5(signContent + key).toUpperCase();
    }

    /**
     * 生成支付签名（HMAC-SHA256，更安全的替代方案）
     * <p>需要支付网关支持 HMAC-SHA256 签名算法。</p>
     */
    public String generateSignHmac(Map<String, String> params, String key) {
        try {
            String signContent = buildSignContent(params);
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] result = mac.doFinal(signContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : result) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().toUpperCase();
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 签名计算失败", e);
        }
    }

    /** 构建签名内容：参数按 key 排序拼接为 key=value&key=value 格式 */
    private String buildSignContent(Map<String, String> params) {
        TreeMap<String, String> sortedParams = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()
                    && !"sign".equals(entry.getKey()) && !"sign_type".equals(entry.getKey())) {
                if (sb.length() > 0) sb.append("&");
                sb.append(entry.getKey()).append("=").append(entry.getValue());
            }
        }
        return sb.toString();
    }

    /** 验证 MD5 签名 */
    public boolean verifySign(Map<String, String> params, String key) {
        String sign = params.get("sign");
        if (sign == null) return false;
        String calculated = generateSign(params, key);
        return sign.equalsIgnoreCase(calculated);
    }

    /** 验证 HMAC-SHA256 签名 */
    public boolean verifySignHmac(Map<String, String> params, String key) {
        String sign = params.get("sign");
        if (sign == null) return false;
        String calculated = generateSignHmac(params, key);
        return sign.equalsIgnoreCase(calculated);
    }

    public String generateOrderNo() {
        // 使用时间戳+8位随机数，降低并发订单号碰撞风险
        return "ORD" + System.currentTimeMillis() + String.format("%08d", new SecureRandom().nextInt(100000000));
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("MD5 哈希计算失败", e);
        }
    }
}
