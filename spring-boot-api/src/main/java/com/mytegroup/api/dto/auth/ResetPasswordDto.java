package com.mytegroup.api.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordDto {
    
    @NotBlank(message = "Token is required")
    private String token;
    
    @NotBlank(message = "New password is required")
    @Pattern(regexp = PasswordRules.STRONG_PASSWORD_REGEX, message = PasswordRules.STRONG_PASSWORD_MESSAGE)
    private String newPassword;
}
