package com.mytegroup.api.dto.waitlist;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record InviteBatchDto(
    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 100, message = "Limit must be at most 100")
    Integer limit,
    @Size(max = 100, message = "Cohort tag must be at most 100 characters")
    String cohortTag
) {
    public InviteBatchDto {
        if (cohortTag != null) {
            cohortTag = cohortTag.trim();
        }
    }
}
