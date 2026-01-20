package com.mytegroup.api.dto.auth;

import com.mytegroup.api.common.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record RegisterDto(
    String firstName,
    String lastName,
    @NotBlank(message = "Username is required")
    String username,
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,
    @NotBlank(message = "Password is required")
    @Pattern(regexp = PasswordRules.STRONG_PASSWORD_REGEX, message = PasswordRules.STRONG_PASSWORD_MESSAGE)
    String password,
    String orgId,
    String organizationName,
    Role role,
    String inviteToken,
    @NotNull(message = "Legal acceptance is required")
    Boolean legalAccepted,
    Boolean orgLegalReconfirm
) {
    public RegisterDto {
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
        if (organizationName != null) {
            organizationName = organizationName.trim();
        }
    }
}

