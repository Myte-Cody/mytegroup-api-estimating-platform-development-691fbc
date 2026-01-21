package com.mytegroup.api.dto.waitlist;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.dto.common.ValidationConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartWaitlistDto {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 120, message = "Name must be between 2 and 120 characters")
    private String name;
    
    @Pattern(regexp = ValidationConstants.E164_PHONE_REGEX, message = ValidationConstants.PHONE_REGEX_MESSAGE)
    private String phone;
    
    private Role role;
    
    @Size(max = 120, message = "Source must be at most 120 characters")
    private String source;
    
    private Boolean preCreateAccount;
    
    private Boolean marketingConsent;
    
    private String trap;
    
    public void setEmail(String email) {
        this.email = email != null ? email.toLowerCase().trim() : null;
    }
    
    public void setName(String name) {
        this.name = name != null ? name.trim() : null;
    }
    
    public void setPhone(String phone) {
        this.phone = phone != null ? phone.trim() : null;
    }
    
    public void setSource(String source) {
        this.source = source != null ? source.trim() : null;
    }
    
    public void setTrap(String trap) {
        this.trap = trap != null ? trap.trim() : null;
    }
}
