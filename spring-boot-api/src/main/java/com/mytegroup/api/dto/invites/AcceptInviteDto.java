package com.mytegroup.api.dto.invites;

import com.mytegroup.api.dto.auth.PasswordRules;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcceptInviteDto {
    
    @NotBlank(message = "Token is required")
    private String token;
    
    @NotBlank(message = "Username is required")
    private String username;
    
    @NotBlank(message = "Password is required")
    @Pattern(regexp = PasswordRules.STRONG_PASSWORD_REGEX, message = PasswordRules.STRONG_PASSWORD_MESSAGE)
    private String password;
    
    public void setUsername(String username) {
        this.username = username != null ? username.trim() : null;
    }
}
