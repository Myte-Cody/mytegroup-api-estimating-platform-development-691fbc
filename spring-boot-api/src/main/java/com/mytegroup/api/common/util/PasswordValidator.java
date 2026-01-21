package com.mytegroup.api.common.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Utility class for password strength validation.
 * Mirrors the NestJS password-rules.ts implementation.
 */
public final class PasswordValidator {
    
    /**
     * Strong password regex pattern:
     * - At least 12 characters
     * - At least one lowercase letter
     * - At least one uppercase letter
     * - At least one digit
     * - At least one special character (non-word, non-whitespace)
     */
    public static final Pattern STRONG_PASSWORD_REGEX = 
        Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^\\w\\s]).{12,}$");
    
    public static final String STRONG_PASSWORD_MESSAGE = 
        "Password must be at least 12 characters and include uppercase, lowercase, number, and symbol.";
    
    public static final int MIN_LENGTH = 12;
    
    private PasswordValidator() {
        // Utility class
    }
    
    /**
     * Checks if a password meets the strong password requirements.
     * @param password the password to validate
     * @return true if the password is strong
     */
    public static boolean isStrong(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        return STRONG_PASSWORD_REGEX.matcher(password).matches();
    }
    
    /**
     * Validates a password and throws an exception if it's not strong enough.
     * @param password the password to validate
     * @throws IllegalArgumentException if the password is weak
     */
    public static void validateStrong(String password) {
        if (!isStrong(password)) {
            throw new IllegalArgumentException(STRONG_PASSWORD_MESSAGE);
        }
    }
    
    /**
     * Gets detailed feedback about password strength.
     * This is a simplified version of zxcvbn feedback.
     * @param password the password to analyze
     * @return PasswordStrengthResult with score and feedback
     */
    public static PasswordStrengthResult getStrength(String password) {
        if (password == null) {
            password = "";
        }
        
        int score = 0;
        List<String> suggestions = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Length checks
        int length = password.length();
        if (length >= 8) score++;
        if (length >= 12) score++;
        if (length >= 16) score++;
        if (length < 12) {
            suggestions.add("Use at least 12 characters");
        }
        
        // Character class checks
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c));
        
        if (hasLower) score++;
        if (hasUpper) score++;
        if (hasDigit) score++;
        if (hasSpecial) score++;
        
        if (!hasLower) suggestions.add("Add lowercase letters");
        if (!hasUpper) suggestions.add("Add uppercase letters");
        if (!hasDigit) suggestions.add("Add numbers");
        if (!hasSpecial) suggestions.add("Add special characters (!@#$%^&*)");
        
        // Common patterns to avoid
        if (containsCommonPatterns(password)) {
            score = Math.max(0, score - 2);
            warnings.add("Avoid common patterns like '123', 'abc', or 'qwerty'");
        }
        
        // Repeated characters
        if (hasRepeatedCharacters(password)) {
            score = Math.max(0, score - 1);
            warnings.add("Avoid repeated characters");
        }
        
        // Normalize score to 0-4 range (matching zxcvbn)
        int normalizedScore = Math.min(4, score / 2);
        
        // Estimate crack time display
        String crackTimeDisplay = estimateCrackTime(normalizedScore);
        
        Map<String, Object> feedback = new HashMap<>();
        feedback.put("suggestions", suggestions);
        feedback.put("warning", warnings.isEmpty() ? null : String.join(". ", warnings));
        
        return new PasswordStrengthResult(
            normalizedScore,
            crackTimeDisplay,
            feedback,
            calculateGuessesLog10(password)
        );
    }
    
    private static boolean containsCommonPatterns(String password) {
        String lower = password.toLowerCase();
        String[] commonPatterns = {
            "123456", "password", "qwerty", "abc123", "111111",
            "12345678", "letmein", "welcome", "monkey", "dragon",
            "master", "login", "admin", "passw0rd"
        };
        for (String pattern : commonPatterns) {
            if (lower.contains(pattern)) {
                return true;
            }
        }
        // Check for sequential patterns
        return lower.contains("123") || lower.contains("abc") || lower.contains("qwe");
    }
    
    private static boolean hasRepeatedCharacters(String password) {
        if (password.length() < 3) return false;
        for (int i = 0; i < password.length() - 2; i++) {
            if (password.charAt(i) == password.charAt(i + 1) && 
                password.charAt(i) == password.charAt(i + 2)) {
                return true;
            }
        }
        return false;
    }
    
    private static String estimateCrackTime(int score) {
        return switch (score) {
            case 0 -> "instantly";
            case 1 -> "minutes";
            case 2 -> "hours";
            case 3 -> "days";
            case 4 -> "centuries";
            default -> "unknown";
        };
    }
    
    private static double calculateGuessesLog10(String password) {
        // Simplified entropy calculation
        int charsetSize = 0;
        if (password.chars().anyMatch(Character::isLowerCase)) charsetSize += 26;
        if (password.chars().anyMatch(Character::isUpperCase)) charsetSize += 26;
        if (password.chars().anyMatch(Character::isDigit)) charsetSize += 10;
        if (password.chars().anyMatch(c -> !Character.isLetterOrDigit(c))) charsetSize += 32;
        
        if (charsetSize == 0) return 0;
        
        double entropy = password.length() * (Math.log(charsetSize) / Math.log(2));
        return entropy * Math.log10(2);
    }
    
    /**
     * Result of password strength analysis.
     */
    public record PasswordStrengthResult(
        int score,
        String crackTimesDisplay,
        Map<String, Object> feedback,
        double guessesLog10
    ) {}
}

