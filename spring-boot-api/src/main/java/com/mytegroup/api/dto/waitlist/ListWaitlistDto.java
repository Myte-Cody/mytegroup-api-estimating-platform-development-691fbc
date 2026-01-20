package com.mytegroup.api.dto.waitlist;

import com.mytegroup.api.entity.enums.core.WaitlistStatus;
import com.mytegroup.api.entity.enums.core.WaitlistVerifyStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record ListWaitlistDto(
    WaitlistStatus status,
    WaitlistVerifyStatus verifyStatus,
    @Size(max = 100, message = "Cohort tag must be at most 100 characters")
    String cohortTag,
    @Size(max = 255, message = "Email contains must be at most 255 characters")
    String emailContains,
    @Min(value = 1, message = "Page must be at least 1")
    Integer page,
    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 100, message = "Limit must be at most 100")
    Integer limit
) {
    public ListWaitlistDto {
        if (cohortTag != null) {
            cohortTag = cohortTag.trim();
        }
        if (emailContains != null) {
            emailContains = emailContains.trim();
        }
    }
}
