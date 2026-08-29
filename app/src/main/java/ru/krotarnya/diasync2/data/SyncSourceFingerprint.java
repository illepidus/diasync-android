package ru.krotarnya.diasync2.data;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class SyncSourceFingerprint {
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private SyncSourceFingerprint() {
    }

    static String from(String baseUrl) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(baseUrl.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                int unsigned = value & 0xff;
                result.append(HEX[unsigned >>> 4]);
                result.append(HEX[unsigned & 0x0f]);
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
