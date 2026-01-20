package com.mytegroup.api.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordDto(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email
) {
    public ForgotPasswordDto {
        if (email != null) {
            email = email.toLowerCase().trim();
        }
    }
}

