package com.mytegroup.api.dto.waitlist;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApproveWaitlistDto(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,
    @Size(max = 100, message = "Cohort tag must be at most 100 characters")
    String cohortTag
) {
    public ApproveWaitlistDto {
        if (email != null) {
            email = email.toLowerCase().trim();
        }
        if (cohortTag != null) {
            cohortTag = cohortTag.trim();
        }
    }
}
