package com.mytegroup.api.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginDto(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,

    @NotBlank(message = "Password is required")
    String password
) {
    public LoginDto {
        if (email != null) {
            email = email.toLowerCase().trim();
        }
    }
}

