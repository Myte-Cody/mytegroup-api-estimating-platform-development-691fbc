package com.mytegroup.api.dto.crewassignments;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateCrewAssignmentDto(
    @NotBlank(message = "Organization ID is required")
    String orgId,
    @NotBlank(message = "Project ID is required")
    String projectId,
    @NotBlank(message = "Person ID is required")
    String personId,
    @NotBlank(message = "Crew ID is required")
    String crewId,
    String roleKey,
    @NotNull(message = "Start date is required")
    LocalDate startDate,
    LocalDate endDate,
    String createdBy
) {
    public CreateCrewAssignmentDto {
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
        if (roleKey != null) {
            roleKey = roleKey.trim();
        }
        if (createdBy != null) {
            createdBy = createdBy.trim();
        }
    }
}
