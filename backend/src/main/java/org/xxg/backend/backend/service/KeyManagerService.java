package org.xxg.backend.backend.service;

import org.springframework.stereotype.Service;
import org.xxg.backend.backend.util.AdvancedCryptoUtil;
import java.io.*;
import java.nio.file.*;

@Service
public class KeyManagerService {
    private final AdvancedCryptoUtil cryptoUtil;
    private static final String KEY_DIR = "keys";

    public KeyManagerService(AdvancedCryptoUtil cryptoUtil) { this.cryptoUtil = cryptoUtil; }

    public String generateAndSaveKeys() throws Exception {
        new File(KEY_DIR).mkdirs();
        String keyPair = cryptoUtil.generateEccKeyPair();
        String[] parts = keyPair.split("\\|");
        Files.writeString(Paths.get(KEY_DIR, "public.key"), parts[0]);
        Files.writeString(Paths.get(KEY_DIR, "private.key"), parts[1]);
        return keyPair;
    }

    public String getPublicKey() throws Exception {
        Path path = Paths.get(KEY_DIR, "public.key");
        return Files.exists(path) ? Files.readString(path) : null;
    }

    public String getPrivateKey() throws Exception {
        Path path = Paths.get(KEY_DIR, "private.key");
        return Files.exists(path) ? Files.readString(path) : null;
    }
}
