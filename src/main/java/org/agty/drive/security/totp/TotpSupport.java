package org.agty.drive.security.totp;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

public final class TotpSupport {

    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private TotpSupport() {
    }

    public static String generateSecret() {
        byte[] randomBytes = new byte[20];
        SECURE_RANDOM.nextBytes(randomBytes);
        return toBase32(randomBytes);
    }

    public static String buildOtpAuthUri(String issuer, String accountName, String secret) {
        String encodedIssuer = urlEncode(issuer);
        String encodedLabel = urlEncode(issuer + ":" + accountName);
        return "otpauth://totp/" + encodedLabel + "?secret=" + secret + "&issuer=" + encodedIssuer;
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String toBase32(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        int current = 0;
        int bitsRemaining = 0;

        for (byte sourceByte : bytes) {
            current = (current << 8) | (sourceByte & 0xFF);
            bitsRemaining += 8;

            while (bitsRemaining >= 5) {
                int index = (current >> (bitsRemaining - 5)) & 0x1F;
                bitsRemaining -= 5;
                result.append(BASE32_ALPHABET.charAt(index));
            }
        }

        if (bitsRemaining > 0) {
            int index = (current << (5 - bitsRemaining)) & 0x1F;
            result.append(BASE32_ALPHABET.charAt(index));
        }

        return result.toString();
    }
}
