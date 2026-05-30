package org.xxg.backend.backend.util;

import org.springframework.stereotype.Component;
import java.util.*;
import java.security.MessageDigest;
import java.security.SecureRandom;

@Component
public class PaymentUtil {

    public String generateSign(Map<String, String> params, String key) {
        // Sort params by key, concatenate as key=value&key=value, append key
        TreeMap<String, String> sortedParams = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()
                    && !"sign".equals(entry.getKey()) && !"sign_type".equals(entry.getKey())) {
                if (sb.length() > 0) sb.append("&");
                sb.append(entry.getKey()).append("=").append(entry.getValue());
            }
        }
        sb.append(key);
        return md5(sb.toString()).toUpperCase();
    }

    public boolean verifySign(Map<String, String> params, String key) {
        String sign = params.get("sign");
        if (sign == null) return false;
        String calculated = generateSign(params, key);
        return sign.equalsIgnoreCase(calculated);
    }

    public String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + String.format("%04d", new SecureRandom().nextInt(10000));
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("MD5 error", e);
        }
    }
}
