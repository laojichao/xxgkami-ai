package org.xxg.backend.backend.service;

import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class TotpService {

    public String generateSecret() {
        byte[] buffer = new byte[20];
        new SecureRandom().nextBytes(buffer);
        return Base64.getEncoder().encodeToString(buffer);
    }

    public String generateCode(String secret) {
        try {
            byte[] key = Base64.getDecoder().decode(secret);
            long time = System.currentTimeMillis() / 30000;
            byte[] timeBytes = ByteBuffer.allocate(8).putLong(time).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(timeBytes);
            int offset = hash[hash.length - 1] & 0x0F;
            int code = ((hash[offset] & 0x7F) << 24) | ((hash[offset + 1] & 0xFF) << 16) |
                       ((hash[offset + 2] & 0xFF) << 8) | (hash[offset + 3] & 0xFF);
            return String.format("%06d", code % 1000000);
        } catch (Exception e) {
            return "000000";
        }
    }

    public boolean verifyCode(String secret, String code) {
        return generateCode(secret).equals(code);
    }
}
