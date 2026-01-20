package com.mytegroup.api.dto.crmcontext;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ListCrmContextDocumentsQueryDto(
    String orgId,
    @NotNull(message = "Entity type is required")
    String entityType,
    Boolean includeArchived,
    @Min(value = 1, message = "Page must be at least 1")
    Integer page,
    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 250, message = "Limit must be at most 250")
    Integer limit
) {
    public ListCrmContextDocumentsQueryDto {
        if (orgId != null) {
            orgId = orgId.trim();
        }
        if (entityType != null) {
            entityType = entityType.trim();
        }
    }
}

