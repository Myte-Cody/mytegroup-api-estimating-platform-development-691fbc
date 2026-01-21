package com.mytegroup.api.dto.users;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.dto.auth.PasswordRules;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserDto {
    
    @NotBlank(message = "Username is required")
    private String username;
    
    private String firstName;
    
    private String lastName;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Pattern(regexp = PasswordRules.STRONG_PASSWORD_REGEX, message = PasswordRules.STRONG_PASSWORD_MESSAGE)
    private String password;
    
    private Role role;
    
    private List<Role> roles;
    
    private String orgId;
    
    private String verificationTokenHash;
    
    private LocalDateTime verificationTokenExpires;
    
    private String resetTokenHash;
    
    private LocalDateTime resetTokenExpires;
    
    private Boolean isEmailVerified;
    
    private Boolean isOrgOwner;
    
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
}
