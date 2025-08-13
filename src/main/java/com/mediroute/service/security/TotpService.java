package com.mediroute.service.security;

import org.apache.commons.codec.binary.Base32;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.time.Instant;

public class TotpService {
    private static final Base32 BASE32 = new Base32();
    private static final SecureRandom RNG = new SecureRandom();

    /** 20-byte random secret, Base32-encoded (RFC 3548). */
    public static String generateBase32Secret() {
        byte[] buf = new byte[20];
        RNG.nextBytes(buf);
        return BASE32.encodeToString(buf).replace("=", "");
    }

    /** Validate TOTP code allowing +/- one step (~30s window). */
    public static boolean validateCode(String base32Secret, String code) {
        if (base32Secret == null || base32Secret.isBlank() || code == null || code.length() < 6) return false;
        long t = Instant.now().getEpochSecond() / 30;
        try {
            int c = Integer.parseInt(code);
            for (long offset = -1; offset <= 1; offset++) {
                if (c == totpFor(base32Secret, t + offset)) return true;
            }
        } catch (NumberFormatException ignore) {}
        return false;
    }

    /** otpauth:// URI (front-end can render QR from this) */
    public static String buildOtpAuthUri(String issuer, String labelEmail, String secret) {
        String label = urlEncode(issuer) + ":" + urlEncode(labelEmail);
        return "otpauth://totp/" + label + "?secret=" + secret + "&issuer=" + urlEncode(issuer) + "&digits=6&period=30";
    }

    /* ---------- internals ---------- */

    private static int totpFor(String base32Secret, long timestep) {
        byte[] key = BASE32.decode(base32Secret.toUpperCase());
        byte[] msg = new byte[8];
        for (int i = 7; i >= 0; --i) { msg[i] = (byte)(timestep & 0xFF); timestep >>= 8; }
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(msg);
            int off = hash[hash.length - 1] & 0x0F;
            int bin = ((hash[off] & 0x7F) << 24) | ((hash[off+1] & 0xFF) << 16) | ((hash[off+2] & 0xFF) << 8) | (hash[off+3] & 0xFF);
            return bin % 1_000_000;
        } catch (Exception e) {
            throw new IllegalStateException("TOTP failure", e);
        }
    }

    private static String urlEncode(String s) {
        try { return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8); }
        catch (Exception e) { return s; }
    }
}