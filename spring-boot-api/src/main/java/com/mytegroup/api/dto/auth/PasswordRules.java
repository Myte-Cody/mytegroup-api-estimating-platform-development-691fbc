package com.mytegroup.api.dto.auth;

public class PasswordRules {
    public static final String STRONG_PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^\\w\\s]).{12,}$";
    public static final String STRONG_PASSWORD_MESSAGE = "Password must be at least 12 characters and include uppercase, lowercase, number, and symbol.";
}



