package com.mytegroup.api.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Utility class for generating and hashing tokens.
 * Uses SHA-256 for hashing (matching NestJS crypto implementation).
 */
public final class TokenHashUtil {
    
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final HexFormat HEX_FORMAT = HexFormat.of();
    
    private TokenHashUtil() {
        // Utility class
    }
    
    /**
     * Generates a cryptographically secure random token as hex string.
     * @return 64-character hex string (32 bytes)
     */
    public static String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return HEX_FORMAT.formatHex(bytes);
    }
    
    /**
     * Generates a token and its SHA-256 hash.
     * @param ttlMs time-to-live in milliseconds
     * @return TokenData containing token, hash, and expiry
     */
    public static TokenData generateTokenWithHash(long ttlMs) {
        String token = generateToken();
        String hash = hashToken(token);
        LocalDateTime expires = LocalDateTime.now().plusNanos(ttlMs * 1_000_000);
        return new TokenData(token, hash, expires);
    }
    
    /**
     * Generates a token with hash and expiry in hours.
     * @param ttlHours time-to-live in hours
     * @return TokenData containing token, hash, and expiry
     */
    public static TokenData generateTokenWithHashHours(int ttlHours) {
        String token = generateToken();
        String hash = hashToken(token);
        LocalDateTime expires = LocalDateTime.now().plusHours(ttlHours);
        return new TokenData(token, hash, expires);
    }
    
    /**
     * Hashes a token using SHA-256.
     * @param token the token to hash
     * @return hex-encoded SHA-256 hash
     */
    public static String hashToken(String token) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HEX_FORMAT.formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    
    /**
     * Verifies a token against a hash.
     * @param token the token to verify
     * @param expectedHash the expected SHA-256 hash
     * @return true if the token matches the hash
     */
    public static boolean verifyToken(String token, String expectedHash) {
        if (token == null || expectedHash == null) {
            return false;
        }
        String actualHash = hashToken(token);
        return MessageDigest.isEqual(
            actualHash.getBytes(StandardCharsets.UTF_8),
            expectedHash.getBytes(StandardCharsets.UTF_8)
        );
    }
    
    /**
     * Generates a numeric verification code.
     * @param length the number of digits
     * @return numeric code as string
     */
    public static String generateNumericCode(int length) {
        if (length <= 0 || length > 10) {
            throw new IllegalArgumentException("Code length must be between 1 and 10");
        }
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            code.append(SECURE_RANDOM.nextInt(10));
        }
        return code.toString();
    }
    
    /**
     * Generates a 6-digit verification code.
     * @return 6-digit numeric code
     */
    public static String generateVerificationCode() {
        return generateNumericCode(6);
    }
    
    /**
     * Data class holding token, hash, and expiry.
     */
    public record TokenData(String token, String hash, LocalDateTime expires) {}
}

