package com.mytegroup.api.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record PasswordStrengthDto(
    @NotBlank(message = "Password is required")
    String password
) {
}

