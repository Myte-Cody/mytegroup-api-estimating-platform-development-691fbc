package com.mytegroup.api.dto.users;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserDto {
    
    private String username;
    
    private String firstName;
    
    private String lastName;
    
    @Email(message = "Email must be valid")
    private String email;
    
    private Boolean isEmailVerified;
    
    private Boolean piiStripped;
    
    private Boolean legalHold;
    
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
