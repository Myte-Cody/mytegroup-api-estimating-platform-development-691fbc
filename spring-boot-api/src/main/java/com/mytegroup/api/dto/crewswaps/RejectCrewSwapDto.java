package com.mytegroup.api.dto.crewswaps;

import jakarta.validation.constraints.NotBlank;

public record RejectCrewSwapDto(
    String approverId,
    @NotBlank(message = "Rejection reason is required")
    String reason
) {
    public RejectCrewSwapDto {
        if (approverId != null) {
            approverId = approverId.trim();
        }
        if (reason != null) {
            reason = reason.trim();
        }
    }
}
