package com.mytegroup.api.dto.contactinquiries;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerifyContactInquiryDto {
    
    @Size(min = 2, max = 120, message = "Name must be between 2 and 120 characters")
    private String name;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    @NotBlank(message = "Code is required")
    private String code;
    
    private String trap;
    
    public void setName(String name) {
        this.name = name != null ? name.trim() : null;
    }
    
    public void setEmail(String email) {
        this.email = email != null ? email.toLowerCase().trim() : null;
    }
}
