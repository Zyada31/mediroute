package com.mediroute.service.security;

import java.security.spec.*;
import java.util.Base64;

import java.io.BufferedReader;
import java.io.StringReader;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.interfaces.RSAPrivateKey;

public final class PemUtils {
    private PemUtils() {}

    public static RSAPrivateKey readPrivateKey(String pem) {
        if (pem == null || pem.isBlank()) throw new IllegalArgumentException("Empty private key PEM");
        try {
            String normalized = stripPemHeaders(pem);
            byte[] bytes = Base64.getDecoder().decode(normalized);

            // Try PKCS#8 first
            try {
                PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
                return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
            } catch (Exception ignore) {
                // Try PKCS#1 -> wrap into PKCS#8
                RSAPrivateCrtKeySpec rsaSpec = pkcs1ToRsaPrivateCrtSpec(bytes);
                return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(rsaSpec);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse RSA private key PEM", e);
        }
    }

    public static RSAPublicKey readPublicKey(String pem) {
        if (pem == null || pem.isBlank()) throw new IllegalArgumentException("Empty public key PEM");
        try {
            String normalized = stripPemHeaders(pem);
            byte[] bytes = Base64.getDecoder().decode(normalized);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse RSA public key PEM", e);
        }
    }

    private static String stripPemHeaders(String pem) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new StringReader(pem))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("-----")) continue;
                sb.append(line.trim());
            }
        }
        return sb.toString();
    }

    // Minimal PKCS#1 RSA private key DER parser (no external deps)
    // Converts PKCS#1 to RSAPrivateCrtKeySpec.
    private static RSAPrivateCrtKeySpec pkcs1ToRsaPrivateCrtSpec(byte[] pkcs1) throws Exception {
        // very small ASN.1 parser for PKCS#1
        DerReader r = new DerReader(pkcs1);
        r.readSequence(); // top sequence
        r.readInteger(); // version
        BigInteger n  = r.readInteger();
        BigInteger e  = r.readInteger();
        BigInteger d  = r.readInteger();
        BigInteger p  = r.readInteger();
        BigInteger q  = r.readInteger();
        BigInteger dp = r.readInteger();
        BigInteger dq = r.readInteger();
        BigInteger qi = r.readInteger();
        return new RSAPrivateCrtKeySpec(n, e, d, p, q, dp, dq, qi);
    }

    // Tiny DER reader (enough for PKCS#1)
    private static final class DerReader {
        private final byte[] b;
        private int pos = 0;
        DerReader(byte[] b) { this.b = b; }

        void readSequence() { readTag(0x30); readLen(); }
        BigInteger readInteger() {
            readTag(0x02);
            int len = readLen();
            byte[] v = readBytes(len);
            return new BigInteger(v);
        }
        private void readTag(int expected) {
            int tag = b[pos++] & 0xff;
            if (tag != expected) throw new IllegalArgumentException("Unexpected DER tag: " + tag);
        }
        private int readLen() {
            int l = b[pos++] & 0xff;
            if (l < 0x80) return l;
            int num = l & 0x7f;
            int val = 0;
            for (int i=0;i<num;i++) { val = (val<<8) | (b[pos++] & 0xff); }
            return val;
        }
        private byte[] readBytes(int len) {
            byte[] out = new byte[len];
            System.arraycopy(b, pos, out, 0, len);
            pos += len;
            return out;
        }
    }
}