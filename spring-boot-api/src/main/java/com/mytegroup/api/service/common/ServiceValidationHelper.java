package com.mytegroup.api.service.common;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Helper class for common validation and normalization utilities used across services.
 * Provides methods for email, phone, name, key normalization and date parsing.
 */
@Component
public class ServiceValidationHelper {
    
    private static final Pattern PHONE_E164_PATTERN = Pattern.compile("^\\+[1-9]\\d{1,14}$");
    
    /**
     * Normalizes an email address (trim and lowercase)
     */
    public String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase();
    }
    
    /**
     * Normalizes a phone number to E.164 format
     * Returns null if phone cannot be normalized
     */
    public String normalizePhoneE164(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return null;
        }
        
        String cleaned = phone.replaceAll("[\\s().-]", "");
        
        // If already starts with +, validate format
        if (cleaned.startsWith("+")) {
            String digits = cleaned.substring(1);
            if (PHONE_E164_PATTERN.matcher(cleaned).matches()) {
                return cleaned;
            }
            return null;
        }
        
        // Extract digits only
        String digitsOnly = cleaned.replaceAll("\\D", "");
        
        // US/Canada: 10 digits -> +1XXXXXXXXXX
        if (digitsOnly.length() == 10) {
            return "+1" + digitsOnly;
        }
        
        // US/Canada: 11 digits starting with 1 -> +XXXXXXXXXXX
        if (digitsOnly.length() == 11 && digitsOnly.startsWith("1")) {
            return "+" + digitsOnly;
        }
        
        return null;
    }
    
    /**
     * Normalizes a name (trim, lowercase, collapse whitespace)
     */
    public String normalizeName(String name) {
        if (name == null) {
            return null;
        }
        return name.trim()
            .toLowerCase()
            .replaceAll("\\s+", " ");
    }
    
    /**
     * Normalizes a key (trim, lowercase, replace non-alphanumeric with underscore)
     */
    public String normalizeKey(String key) {
        if (key == null) {
            return null;
        }
        return key.trim()
            .toLowerCase()
            .replaceAll("[^a-z0-9_]+", "_")
            .replaceAll("^_+|_+$", "")
            .replaceAll("_+", "_");
    }
    
    /**
     * Normalizes a list of keys, removing duplicates and nulls
     */
    public List<String> normalizeKeys(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return new ArrayList<>();
        }
        
        Set<String> seen = new HashSet<>();
        List<String> output = new ArrayList<>();
        
        for (String key : keys) {
            if (key == null) {
                continue;
            }
            String normalized = normalizeKey(String.valueOf(key));
            if (normalized == null || normalized.isEmpty()) {
                continue;
            }
            if (!seen.contains(normalized)) {
                seen.add(normalized);
                output.add(normalized);
            }
        }
        
        return output;
    }
    
    /**
     * Parses an optional date string to LocalDate
     * Returns null if date is null, empty, or invalid
     */
    public LocalDate parseOptionalDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Try ISO date format (YYYY-MM-DD)
            return LocalDate.parse(dateStr.trim());
        } catch (DateTimeParseException e) {
            // Try other common formats
            DateTimeFormatter[] formatters = {
                DateTimeFormatter.ISO_DATE_TIME,
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            };
            
            for (DateTimeFormatter formatter : formatters) {
                try {
                    LocalDateTime dateTime = LocalDateTime.parse(dateStr.trim(), formatter);
                    return dateTime.toLocalDate();
                } catch (DateTimeParseException ignored) {
                    // Try next format
                }
            }
            
            return null;
        }
    }
    
    /**
     * Parses an optional date string to LocalDateTime
     * Returns null if date is null, empty, or invalid
     */
    public LocalDateTime parseOptionalDateTime(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Try ISO date-time format
            return LocalDateTime.parse(dateStr.trim());
        } catch (DateTimeParseException e) {
            // Try other common formats
            DateTimeFormatter[] formatters = {
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ISO_DATE_TIME
            };
            
            for (DateTimeFormatter formatter : formatters) {
                try {
                    return LocalDateTime.parse(dateStr.trim(), formatter);
                } catch (DateTimeParseException ignored) {
                    // Try next format
                }
            }
            
            return null;
        }
    }
    
    /**
     * Parses an optional number, returns null if invalid
     */
    public Double parseOptionalNumber(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        
        try {
            double num = Double.parseDouble(value.trim());
            if (Double.isFinite(num)) {
                return num;
            }
            return null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * Parses an optional integer, returns null if invalid
     */
    public Integer parseOptionalInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * Deduplicates a list of strings, trimming and filtering empty values
     */
    public List<String> dedupeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }
        
        Set<String> seen = new HashSet<>();
        List<String> output = new ArrayList<>();
        
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!seen.contains(trimmed)) {
                seen.add(trimmed);
                output.add(trimmed);
            }
        }
        
        return output;
    }
}


