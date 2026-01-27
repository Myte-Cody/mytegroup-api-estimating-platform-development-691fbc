package com.mytegroup.api.dto.timesheets;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CreateTimesheetDto(
    @NotBlank(message = "Organization ID is required")
    String orgId,
    @NotBlank(message = "Project ID is required")
    String projectId,
    @NotBlank(message = "Person ID is required")
    String personId,
    String crewId,
    @NotNull(message = "Work date is required")
    LocalDate date,
    @Valid
    List<TimesheetEntryDto> entries,
    String createdBy
) {
    public CreateTimesheetDto {
        if (orgId != null) {
            orgId = orgId.trim();
        }
        if (projectId != null) {
            projectId = projectId.trim();
        }
        if (personId != null) {
            personId = personId.trim();
        }
        if (crewId != null) {
            crewId = crewId.trim();
        }
        if (createdBy != null) {
            createdBy = createdBy.trim();
        }
    }
}
