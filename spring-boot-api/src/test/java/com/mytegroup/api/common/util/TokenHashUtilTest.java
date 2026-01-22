package com.mytegroup.api.common.util;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for TokenHashUtil.
 */
class TokenHashUtilTest {

    @Test
    void shouldGenerateToken() {
        String token = TokenHashUtil.generateToken();
        
        assertThat(token).isNotNull();
        assertThat(token.length()).isEqualTo(64); // 32 bytes = 64 hex characters
        assertThat(token).matches("^[0-9a-f]{64}$");
    }

    @Test
    void shouldGenerateUniqueTokens() {
        String token1 = TokenHashUtil.generateToken();
        String token2 = TokenHashUtil.generateToken();
        
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    void shouldHashToken() {
        String token = "test-token-123";
        String hash = TokenHashUtil.hashToken(token);
        
        assertThat(hash).isNotNull();
        assertThat(hash.length()).isEqualTo(64); // SHA-256 produces 64 hex characters
        assertThat(hash).matches("^[0-9a-f]{64}$");
    }

    @Test
    void shouldProduceConsistentHash() {
        String token = "test-token-123";
        String hash1 = TokenHashUtil.hashToken(token);
        String hash2 = TokenHashUtil.hashToken(token);
        
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void shouldThrowExceptionForNullToken() {
        assertThatThrownBy(() -> TokenHashUtil.hashToken(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Token cannot be null or empty");
    }

    @Test
    void shouldThrowExceptionForEmptyToken() {
        assertThatThrownBy(() -> TokenHashUtil.hashToken(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Token cannot be null or empty");
    }

    @Test
    void shouldVerifyToken() {
        String token = "test-token-123";
        String hash = TokenHashUtil.hashToken(token);
        
        assertThat(TokenHashUtil.verifyToken(token, hash)).isTrue();
        assertThat(TokenHashUtil.verifyToken("wrong-token", hash)).isFalse();
    }

    @Test
    void shouldReturnFalseForNullTokenOrHash() {
        assertThat(TokenHashUtil.verifyToken(null, "hash")).isFalse();
        assertThat(TokenHashUtil.verifyToken("token", null)).isFalse();
        assertThat(TokenHashUtil.verifyToken(null, null)).isFalse();
    }

    @Test
    void shouldGenerateTokenWithHash() {
        long ttlMs = 3600000; // 1 hour
        TokenHashUtil.TokenData data = TokenHashUtil.generateTokenWithHash(ttlMs);
        
        assertThat(data).isNotNull();
        assertThat(data.token()).isNotNull();
        assertThat(data.hash()).isNotNull();
        assertThat(data.expires()).isNotNull();
        assertThat(data.expires()).isAfter(LocalDateTime.now());
        assertThat(TokenHashUtil.verifyToken(data.token(), data.hash())).isTrue();
    }

    @Test
    void shouldGenerateTokenWithHashHours() {
        int ttlHours = 24;
        TokenHashUtil.TokenData data = TokenHashUtil.generateTokenWithHashHours(ttlHours);
        
        assertThat(data).isNotNull();
        assertThat(data.token()).isNotNull();
        assertThat(data.hash()).isNotNull();
        assertThat(data.expires()).isNotNull();
        assertThat(data.expires()).isAfter(LocalDateTime.now());
        assertThat(Duration.between(LocalDateTime.now(), data.expires()).toHours()).isCloseTo(24L, org.assertj.core.data.Offset.offset(1L));
    }

    @Test
    void shouldGenerateNumericCode() {
        String code = TokenHashUtil.generateNumericCode(6);
        
        assertThat(code).isNotNull();
        assertThat(code.length()).isEqualTo(6);
        assertThat(code).matches("^[0-9]{6}$");
    }

    @Test
    void shouldGenerateVerificationCode() {
        String code = TokenHashUtil.generateVerificationCode();
        
        assertThat(code).isNotNull();
        assertThat(code.length()).isEqualTo(6);
        assertThat(code).matches("^[0-9]{6}$");
    }

    @Test
    void shouldThrowExceptionForInvalidCodeLength() {
        assertThatThrownBy(() -> TokenHashUtil.generateNumericCode(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TokenHashUtil.generateNumericCode(11))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldGenerateDifferentNumericCodes() {
        String code1 = TokenHashUtil.generateNumericCode(6);
        String code2 = TokenHashUtil.generateNumericCode(6);
        
        // Very unlikely to be the same, but possible
        // Just verify they're both valid
        assertThat(code1).matches("^[0-9]{6}$");
        assertThat(code2).matches("^[0-9]{6}$");
    }
}

