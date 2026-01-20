package com.mytegroup.api.dto.users;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.dto.auth.PasswordRules;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;
import java.util.List;

public record CreateUserDto(
    @NotBlank(message = "Username is required")
    String username,
    String firstName,
    String lastName,
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,
    @NotBlank(message = "Password is required")
    @Pattern(regexp = PasswordRules.STRONG_PASSWORD_REGEX, message = PasswordRules.STRONG_PASSWORD_MESSAGE)
    String password,
    Role role,
    List<Role> roles,
    String orgId,
    String verificationTokenHash,
    LocalDateTime verificationTokenExpires,
    String resetTokenHash,
    LocalDateTime resetTokenExpires,
    Boolean isEmailVerified,
    Boolean isOrgOwner
) {
    public CreateUserDto {
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

