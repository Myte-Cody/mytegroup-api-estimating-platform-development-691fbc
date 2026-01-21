package com.mytegroup.api.common.util;

import java.util.Set;

/**
 * Utility class for email domain operations.
 * Mirrors the NestJS domain.util.ts implementation.
 */
public final class DomainUtil {
    
    /**
     * Common personal email domains that may be blocked for business use.
     */
    public static final Set<String> PERSONAL_EMAIL_DOMAINS = Set.of(
        "gmail.com",
        "googlemail.com",
        "yahoo.com",
        "yahoo.co.uk",
        "hotmail.com",
        "hotmail.co.uk",
        "outlook.com",
        "outlook.co.uk",
        "live.com",
        "msn.com",
        "icloud.com",
        "me.com",
        "mac.com",
        "aol.com",
        "protonmail.com",
        "proton.me",
        "mail.com",
        "zoho.com",
        "yandex.com",
        "gmx.com",
        "gmx.net"
    );
    
    private DomainUtil() {
        // Utility class
    }
    
    /**
     * Extracts and normalizes the domain from an email address.
     * @param email the email address
     * @return the lowercase domain, or null if invalid
     */
    public static String normalizeDomainFromEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String trimmed = email.trim().toLowerCase();
        int atIndex = trimmed.indexOf('@');
        if (atIndex <= 0 || atIndex >= trimmed.length() - 1) {
            return null;
        }
        String domain = trimmed.substring(atIndex + 1);
        // Basic validation: must contain at least one dot
        if (!domain.contains(".") || domain.startsWith(".") || domain.endsWith(".")) {
            return null;
        }
        return domain;
    }
    
    /**
     * Normalizes an email address to lowercase and trimmed.
     * @param email the email address
     * @return normalized email, or null if invalid
     */
    public static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String normalized = email.trim().toLowerCase();
        if (!isValidEmail(normalized)) {
            return null;
        }
        return normalized;
    }
    
    /**
     * Basic email validation.
     * @param email the email to validate
     * @return true if the email format is valid
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        // Simple regex matching NestJS pattern: /^[^@\s]+@[^@\s]+\.[^@\s]+$/
        return email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }
    
    /**
     * Checks if an email uses a personal email domain.
     * @param email the email address
     * @return true if the domain is a personal email provider
     */
    public static boolean isPersonalEmailDomain(String email) {
        String domain = normalizeDomainFromEmail(email);
        if (domain == null) {
            return false;
        }
        return PERSONAL_EMAIL_DOMAINS.contains(domain);
    }
    
    /**
     * Checks if an email uses a business/company domain.
     * @param email the email address
     * @return true if the domain is not a personal email provider
     */
    public static boolean isBusinessEmailDomain(String email) {
        String domain = normalizeDomainFromEmail(email);
        if (domain == null) {
            return false;
        }
        return !PERSONAL_EMAIL_DOMAINS.contains(domain);
    }
    
    /**
     * Validates a phone number in E.164 format.
     * @param phone the phone number
     * @return true if valid E.164 format
     */
    public static boolean isValidE164Phone(String phone) {
        if (phone == null || phone.isBlank()) {
            return false;
        }
        // E.164 format: +[country code][number], 1-15 digits total
        return phone.matches("^\\+[1-9]\\d{1,14}$");
    }
    
    /**
     * Normalizes a phone number by trimming whitespace.
     * @param phone the phone number
     * @return trimmed phone number
     */
    public static String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        return phone.trim();
    }
}

