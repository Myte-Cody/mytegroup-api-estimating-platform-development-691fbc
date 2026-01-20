package com.mytegroup.api.dto.invites;

import com.mytegroup.api.dto.auth.PasswordRules;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AcceptInviteDto(
    @NotBlank(message = "Token is required")
    String token,
    @NotBlank(message = "Username is required")
    String username,
    @NotBlank(message = "Password is required")
    @Pattern(regexp = PasswordRules.STRONG_PASSWORD_REGEX, message = PasswordRules.STRONG_PASSWORD_MESSAGE)
    String password
) {
    public AcceptInviteDto {
        if (username != null) {
            username = username.trim();
        }
    }
}

