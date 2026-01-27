package com.mytegroup.api.dto.crewassignments;

import java.time.LocalDate;

public record UpdateCrewAssignmentDto(
    String crewId,
    String roleKey,
    LocalDate startDate,
    LocalDate endDate,
    String status
) {
    public UpdateCrewAssignmentDto {
        if (crewId != null) {
            crewId = crewId.trim();
        }
        if (roleKey != null) {
            roleKey = roleKey.trim();
        }
        if (status != null) {
            status = status.trim().toLowerCase();
        }
    }
}
