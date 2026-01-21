package com.mytegroup.api.dto.waitlist;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InviteBatchDto {
    
    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 100, message = "Limit must be at most 100")
    private Integer limit;
    
    @Size(max = 100, message = "Cohort tag must be at most 100 characters")
    private String cohortTag;
    
    public void setCohortTag(String cohortTag) {
        this.cohortTag = cohortTag != null ? cohortTag.trim() : null;
    }
}
