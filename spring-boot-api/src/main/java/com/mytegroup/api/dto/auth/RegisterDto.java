package com.mytegroup.api.dto.auth;

import com.mytegroup.api.common.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDto {
    
    private String firstName;
    
    private String lastName;
    
    @NotBlank(message = "Username is required")
    private String username;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Pattern(regexp = PasswordRules.STRONG_PASSWORD_REGEX, message = PasswordRules.STRONG_PASSWORD_MESSAGE)
    private String password;
    
    private String orgId;
    
    private String organizationName;
    
    private Role role;
    
    private String inviteToken;
    
    @NotNull(message = "Legal acceptance is required")
    private Boolean legalAccepted;
    
    private Boolean orgLegalReconfirm;
    
    public void setEmail(String email) {
        this.email = email != null ? email.toLowerCase().trim() : null;
    }
    
    public void setUsername(String username) {
        this.username = username != null ? username.trim() : null;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName != null ? firstName.trim() : null;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName != null ? lastName.trim() : null;
    }
    
    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName != null ? organizationName.trim() : null;
    }
}
