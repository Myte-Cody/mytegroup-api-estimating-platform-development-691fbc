package com.mytegroup.api.dto.timesheets;

import jakarta.validation.Valid;

import java.util.List;

public record UpdateTimesheetDto(
    @Valid
    List<TimesheetEntryDto> entries,
    String status
) {
    public UpdateTimesheetDto {
        if (status != null) {
            status = status.trim().toLowerCase();
        }
    }
}
