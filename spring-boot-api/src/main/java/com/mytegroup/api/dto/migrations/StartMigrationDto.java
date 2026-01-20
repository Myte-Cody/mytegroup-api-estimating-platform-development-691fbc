package com.mytegroup.api.dto.migrations;

import com.mytegroup.api.entity.enums.system.MigrationDirection;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StartMigrationDto(
    @NotBlank(message = "Organization ID is required")
    String orgId,
    @NotNull(message = "Direction is required")
    MigrationDirection direction,
    String targetUri,
    String targetDbName,
    Boolean dryRun,
    Boolean resume,
    Boolean overrideLegalHold,
    @Min(value = 1, message = "Chunk size must be at least 1")
    @Max(value = 5000, message = "Chunk size must be at most 5000")
    Integer chunkSize
) {
    public StartMigrationDto {
        if (orgId != null) {
            orgId = orgId.trim();
        }
        if (targetUri != null) {
            targetUri = targetUri.trim();
        }
        if (targetDbName != null) {
            targetDbName = targetDbName.trim();
        }
    }
}

