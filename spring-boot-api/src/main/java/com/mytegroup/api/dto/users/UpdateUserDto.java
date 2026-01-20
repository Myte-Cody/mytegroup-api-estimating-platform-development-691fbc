package com.mytegroup.api.dto.users;

import jakarta.validation.constraints.Email;

public record UpdateUserDto(
    String username,
    String firstName,
    String lastName,
    @Email(message = "Email must be valid")
    String email,
    Boolean isEmailVerified,
    Boolean piiStripped,
    Boolean legalHold
) {
    public UpdateUserDto {
        if (email != null) {
            email = email.toLowerCase().trim();
        }
        if (username != null) {
            username = username.trim();
        }
        if (firstName != null) {
            firstName = firstName.trim();
        }
        if (lastName != null) {
            lastName = lastName.trim();
        }
    }
}

