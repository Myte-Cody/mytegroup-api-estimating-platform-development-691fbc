package com.mytegroup.api.dto.timesheets;

import jakarta.validation.constraints.NotBlank;

public record RejectTimesheetDto(
    String approverId,
    @NotBlank(message = "Rejection reason is required")
    String reason
) {
    public RejectTimesheetDto {
        if (approverId != null) {
            approverId = approverId.trim();
        }
        if (reason != null) {
            reason = reason.trim();
        }
    }
}
