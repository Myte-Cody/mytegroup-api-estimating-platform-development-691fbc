package com.mytegroup.api.dto.waitlist;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApproveWaitlistDto {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    @Size(max = 100, message = "Cohort tag must be at most 100 characters")
    private String cohortTag;
    
    public void setEmail(String email) {
        this.email = email != null ? email.toLowerCase().trim() : null;
    }
    
    public void setCohortTag(String cohortTag) {
        this.cohortTag = cohortTag != null ? cohortTag.trim() : null;
    }
}
