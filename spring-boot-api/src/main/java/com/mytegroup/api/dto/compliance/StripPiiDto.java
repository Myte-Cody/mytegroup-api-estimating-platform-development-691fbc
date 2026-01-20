package com.mytegroup.api.dto.compliance;

import jakarta.validation.constraints.NotBlank;

public record StripPiiDto(
    @NotBlank(message = "Entity type is required")
    String entityType,
    @NotBlank(message = "Entity ID is required")
    String entityId
) {
    public StripPiiDto {
        if (entityType != null) {
            entityType = entityType.trim();
        }
        if (entityId != null) {
            entityId = entityId.trim();
        }
    }
}

