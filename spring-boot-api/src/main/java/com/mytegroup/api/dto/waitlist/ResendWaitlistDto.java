package com.mytegroup.api.dto.waitlist;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendWaitlistDto(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email
) {
    public ResendWaitlistDto {
        if (email != null) {
            email = email.toLowerCase().trim();
        }
    }
}
