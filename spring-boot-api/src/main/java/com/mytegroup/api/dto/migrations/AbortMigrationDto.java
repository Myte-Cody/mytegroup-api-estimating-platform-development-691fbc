package com.mytegroup.api.dto.migrations;

import jakarta.validation.constraints.NotBlank;

public record AbortMigrationDto(
    @NotBlank(message = "Migration ID is required")
    String migrationId,
    @NotBlank(message = "Organization ID is required")
    String orgId,
    String reason
) {
    public AbortMigrationDto {
        if (migrationId != null) {
            migrationId = migrationId.trim();
        }
        if (orgId != null) {
            orgId = orgId.trim();
        }
        if (reason != null) {
            reason = reason.trim();
        }
    }
}

