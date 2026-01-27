package com.mytegroup.api.dto.timesheets;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TimesheetEntryDto(
    String taskId,
    @NotNull(message = "Hours is required")
    @Min(value = 0, message = "Hours must be non-negative")
    Double hours,
    String hoursType,
    String notes
) {
    public TimesheetEntryDto {
        if (taskId != null) {
            taskId = taskId.trim();
        }
        if (hoursType != null) {
            hoursType = hoursType.trim().toLowerCase();
        }
        if (notes != null) {
            notes = notes.trim();
        }
    }
}
