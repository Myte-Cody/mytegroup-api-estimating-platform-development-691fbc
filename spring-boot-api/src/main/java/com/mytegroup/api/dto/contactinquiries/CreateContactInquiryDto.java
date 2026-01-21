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
public class CreateContactInquiryDto {
    
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 120, message = "Name must be between 2 and 120 characters")
    private String name;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    private String phone;
    
    private String companyName;
    
    @NotBlank(message = "Message is required")
    @Size(min = 10, max = 4000, message = "Message must be between 10 and 4000 characters")
    private String message;
    
    @Size(max = 120, message = "Source must be at most 120 characters")
    private String source;
    
    private String trap;
    
    public void setName(String name) {
        this.name = name != null ? name.trim() : null;
    }
    
    public void setEmail(String email) {
        this.email = email != null ? email.toLowerCase().trim() : null;
    }
    
    public void setMessage(String message) {
        this.message = message != null ? message.trim() : null;
    }
    
    public void setSource(String source) {
        this.source = source != null ? source.trim() : null;
    }
}
