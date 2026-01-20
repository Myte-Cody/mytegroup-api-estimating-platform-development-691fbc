package com.mytegroup.api.dto.migrations;

import jakarta.validation.constraints.NotBlank;

public record FinalizeMigrationDto(
    @NotBlank(message = "Migration ID is required")
    String migrationId,
    @NotBlank(message = "Organization ID is required")
    String orgId,
    Boolean confirmCutover
) {
    public FinalizeMigrationDto {
        if (migrationId != null) {
            migrationId = migrationId.trim();
        }
        if (orgId != null) {
            orgId = orgId.trim();
        }
    }
}

