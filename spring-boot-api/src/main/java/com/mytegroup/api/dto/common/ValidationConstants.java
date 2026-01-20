package com.mytegroup.api.dto.common;

public class ValidationConstants {
    public static final String PHONE_REGEX = "^[0-9+()\\-.\\s]{7,25}$";
    public static final String E164_PHONE_REGEX = "^\\+[1-9]\\d{1,14}$";
    public static final String VERIFICATION_CODE_REGEX = "^\\d{6}$";
    public static final String SNAKE_CASE_KEY_REGEX = "^[a-z0-9_]+$";
    
    private ValidationConstants() {
        // Utility class
    }
}

