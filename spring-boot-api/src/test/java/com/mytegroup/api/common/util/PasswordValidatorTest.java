package com.mytegroup.api.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for PasswordValidator.
 */
class PasswordValidatorTest {

    @Test
    void shouldReturnTrueForStrongPassword() {
        assertThat(PasswordValidator.isStrong("StrongPass123!")).isTrue();
        assertThat(PasswordValidator.isStrong("MySecureP@ssw0rd2024")).isTrue();
        assertThat(PasswordValidator.isStrong("Test1234!@#$")).isTrue();
    }

    @Test
    void shouldReturnFalseForWeakPasswords() {
        assertThat(PasswordValidator.isStrong(null)).isFalse();
        assertThat(PasswordValidator.isStrong("")).isFalse();
        assertThat(PasswordValidator.isStrong("short")).isFalse();
        assertThat(PasswordValidator.isStrong("nouppercase123!")).isFalse();
        assertThat(PasswordValidator.isStrong("NOLOWERCASE123!")).isFalse();
        assertThat(PasswordValidator.isStrong("NoNumbers!")).isFalse();
        assertThat(PasswordValidator.isStrong("NoSpecial123")).isFalse();
    }

    @Test
    void shouldThrowExceptionForWeakPassword() {
        assertThatThrownBy(() -> PasswordValidator.validateStrong("weak"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password must be at least 12 characters");
    }

    @Test
    void shouldNotThrowExceptionForStrongPassword() {
        PasswordValidator.validateStrong("StrongPass123!");
    }

    @Test
    void shouldReturnStrengthScore() {
        PasswordValidator.PasswordStrengthResult result = PasswordValidator.getStrength("StrongPass123!");
        
        assertThat(result).isNotNull();
        assertThat(result.score()).isGreaterThanOrEqualTo(0);
        assertThat(result.score()).isLessThanOrEqualTo(4);
        assertThat(result.feedback()).isNotNull();
    }

    @Test
    void shouldReturnLowScoreForWeakPassword() {
        PasswordValidator.PasswordStrengthResult result = PasswordValidator.getStrength("weak");
        
        assertThat(result.score()).isLessThanOrEqualTo(2);
        assertThat(result.feedback().get("suggestions")).isNotNull();
    }

    @Test
    void shouldReturnHighScoreForStrongPassword() {
        PasswordValidator.PasswordStrengthResult result = PasswordValidator.getStrength("VeryStrongPassword123!@#");
        
        assertThat(result.score()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void shouldHandleNullPassword() {
        PasswordValidator.PasswordStrengthResult result = PasswordValidator.getStrength(null);
        
        assertThat(result).isNotNull();
        assertThat(result.score()).isEqualTo(0);
    }

    @Test
    void shouldDetectCommonPatterns() {
        PasswordValidator.PasswordStrengthResult result1 = PasswordValidator.getStrength("password123");
        PasswordValidator.PasswordStrengthResult result2 = PasswordValidator.getStrength("qwerty123");
        PasswordValidator.PasswordStrengthResult result3 = PasswordValidator.getStrength("abc123456");
        
        assertThat(result1.feedback().get("warning")).isNotNull();
        assertThat(result2.feedback().get("warning")).isNotNull();
        assertThat(result3.feedback().get("warning")).isNotNull();
    }
}

