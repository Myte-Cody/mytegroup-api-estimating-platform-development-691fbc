package com.mytegroup.api.dto.crewswaps;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record CreateCrewSwapDto(
    @NotBlank(message = "Organization ID is required")
    String orgId,
    @NotBlank(message = "Project ID is required")
    String projectId,
    @NotBlank(message = "Person ID is required")
    String personId,
    @NotBlank(message = "From crew ID is required")
    String fromCrewId,
    @NotBlank(message = "To crew ID is required")
    String toCrewId,
    String requestedBy,
    LocalDateTime requestedAt
) {
    public CreateCrewSwapDto {
        if (orgId != null) {
            orgId = orgId.trim();
        }
        if (projectId != null) {
            projectId = projectId.trim();
        }
        if (personId != null) {
            personId = personId.trim();
        }
        if (fromCrewId != null) {
            fromCrewId = fromCrewId.trim();
        }
        if (toCrewId != null) {
            toCrewId = toCrewId.trim();
        }
        if (requestedBy != null) {
            requestedBy = requestedBy.trim();
        }
    }
}
