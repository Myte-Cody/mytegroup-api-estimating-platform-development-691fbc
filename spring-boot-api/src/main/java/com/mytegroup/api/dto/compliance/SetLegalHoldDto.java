package com.mytegroup.api.dto.compliance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SetLegalHoldDto(
    @NotBlank(message = "Entity type is required")
    String entityType,
    @NotBlank(message = "Entity ID is required")
    String entityId,
    @NotNull(message = "Legal hold status is required")
    Boolean legalHold,
    String reason
) {
    public SetLegalHoldDto {
        if (entityType != null) {
            entityType = entityType.trim();
        }
        if (entityId != null) {
            entityId = entityId.trim();
        }
        if (reason != null) {
            reason = reason.trim();
        }
    }
}

