package com.mytegroup.api.dto.compliance;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BatchArchiveDto(
    @NotBlank(message = "Entity type is required")
    String entityType,
    @NotNull(message = "Entity IDs are required")
    @NotEmpty(message = "At least one entity ID is required")
    List<String> entityIds,
    Boolean archive
) {
    public BatchArchiveDto {
        if (entityType != null) {
            entityType = entityType.trim();
        }
    }
}

