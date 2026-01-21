package com.mytegroup.api.dto.waitlist;

import com.mytegroup.api.entity.enums.core.WaitlistStatus;
import com.mytegroup.api.entity.enums.core.WaitlistVerifyStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListWaitlistDto {
    
    private WaitlistStatus status;
    
    private WaitlistVerifyStatus verifyStatus;
    
    @Size(max = 100, message = "Cohort tag must be at most 100 characters")
    private String cohortTag;
    
    @Size(max = 255, message = "Email contains must be at most 255 characters")
    private String emailContains;
    
    @Min(value = 1, message = "Page must be at least 1")
    private Integer page;
    
    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 100, message = "Limit must be at most 100")
    private Integer limit;
    
    public void setCohortTag(String cohortTag) {
        this.cohortTag = cohortTag != null ? cohortTag.trim() : null;
    }
    
    public void setEmailContains(String emailContains) {
        this.emailContains = emailContains != null ? emailContains.trim() : null;
    }
}
