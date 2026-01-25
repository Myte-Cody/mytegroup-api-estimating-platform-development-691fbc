package com.mytegroup.api.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for DomainUtil.
 */
class DomainUtilTest {

    @Test
    void shouldNormalizeDomainFromEmail() {
        assertThat(DomainUtil.normalizeDomainFromEmail("test@example.com")).isEqualTo("example.com");
        assertThat(DomainUtil.normalizeDomainFromEmail("TEST@EXAMPLE.COM")).isEqualTo("example.com");
        assertThat(DomainUtil.normalizeDomainFromEmail("  test@example.com  ")).isEqualTo("example.com");
    }

    @Test
    void shouldReturnNullForInvalidEmails() {
        assertThat(DomainUtil.normalizeDomainFromEmail(null)).isNull();
        assertThat(DomainUtil.normalizeDomainFromEmail("")).isNull();
        assertThat(DomainUtil.normalizeDomainFromEmail("invalid")).isNull();
        assertThat(DomainUtil.normalizeDomainFromEmail("@example.com")).isNull();
        assertThat(DomainUtil.normalizeDomainFromEmail("test@")).isNull();
        assertThat(DomainUtil.normalizeDomainFromEmail("test@.com")).isNull();
    }

    @Test
    void shouldNormalizeEmail() {
        assertThat(DomainUtil.normalizeEmail("Test@Example.COM")).isEqualTo("test@example.com");
        assertThat(DomainUtil.normalizeEmail("  test@example.com  ")).isEqualTo("test@example.com");
    }

    @Test
    void shouldReturnNullForInvalidEmailNormalization() {
        assertThat(DomainUtil.normalizeEmail(null)).isNull();
        assertThat(DomainUtil.normalizeEmail("")).isNull();
        assertThat(DomainUtil.normalizeEmail("invalid")).isNull();
    }

    @Test
    void shouldValidateEmail() {
        assertThat(DomainUtil.isValidEmail("test@example.com")).isTrue();
        assertThat(DomainUtil.isValidEmail("user.name@example.co.uk")).isTrue();
        assertThat(DomainUtil.isValidEmail("test+tag@example.com")).isTrue();
    }

    @Test
    void shouldRejectInvalidEmails() {
        assertThat(DomainUtil.isValidEmail(null)).isFalse();
        assertThat(DomainUtil.isValidEmail("")).isFalse();
        assertThat(DomainUtil.isValidEmail("invalid")).isFalse();
        assertThat(DomainUtil.isValidEmail("@example.com")).isFalse();
        assertThat(DomainUtil.isValidEmail("test@")).isFalse();
        assertThat(DomainUtil.isValidEmail("test @example.com")).isFalse();
    }

    @Test
    void shouldIdentifyPersonalEmailDomains() {
        assertThat(DomainUtil.isPersonalEmailDomain("test@gmail.com")).isTrue();
        assertThat(DomainUtil.isPersonalEmailDomain("test@yahoo.com")).isTrue();
        assertThat(DomainUtil.isPersonalEmailDomain("test@hotmail.com")).isTrue();
        assertThat(DomainUtil.isPersonalEmailDomain("test@outlook.com")).isTrue();
    }

    @Test
    void shouldIdentifyBusinessEmailDomains() {
        assertThat(DomainUtil.isBusinessEmailDomain("test@company.com")).isTrue();
        assertThat(DomainUtil.isBusinessEmailDomain("test@example.org")).isTrue();
        assertThat(DomainUtil.isBusinessEmailDomain("test@business.io")).isTrue();
    }

    @Test
    void shouldReturnFalseForInvalidEmailInPersonalDomainCheck() {
        assertThat(DomainUtil.isPersonalEmailDomain(null)).isFalse();
        assertThat(DomainUtil.isPersonalEmailDomain("invalid")).isFalse();
    }

    @Test
    void shouldValidateE164Phone() {
        assertThat(DomainUtil.isValidE164Phone("+1234567890")).isTrue();
        assertThat(DomainUtil.isValidE164Phone("+441234567890")).isTrue();
        assertThat(DomainUtil.isValidE164Phone("+123456789012345")).isTrue();
    }

    @Test
    void shouldRejectInvalidE164Phone() {
        assertThat(DomainUtil.isValidE164Phone(null)).isFalse();
        assertThat(DomainUtil.isValidE164Phone("")).isFalse();
        assertThat(DomainUtil.isValidE164Phone("1234567890")).isFalse();
        assertThat(DomainUtil.isValidE164Phone("+01234567890")).isFalse();
        assertThat(DomainUtil.isValidE164Phone("+1234567890123456")).isFalse();
    }

    @Test
    void shouldNormalizePhone() {
        assertThat(DomainUtil.normalizePhone("  +1234567890  ")).isEqualTo("+1234567890");
        assertThat(DomainUtil.normalizePhone("+1234567890")).isEqualTo("+1234567890");
    }

    @Test
    void shouldReturnNullForNullPhone() {
        assertThat(DomainUtil.normalizePhone(null)).isNull();
    }
}


