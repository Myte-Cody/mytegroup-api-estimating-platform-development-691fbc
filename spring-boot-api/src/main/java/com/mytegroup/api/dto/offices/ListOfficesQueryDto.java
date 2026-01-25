package com.mytegroup.api.dto.offices;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ListOfficesQueryDto(
    String orgId,
    Boolean includeArchived,
    String search,
    String parentOrgLocationId,
    @Min(value = 1, message = "Page must be at least 1")
    Integer page,
    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 100, message = "Limit must be at most 100")
    Integer limit
) {
    public ListOfficesQueryDto {
        if (orgId != null) {
            orgId = orgId.trim();
        }
        if (search != null) {
            search = search.trim();
        }
        if (parentOrgLocationId != null) {
            parentOrgLocationId = parentOrgLocationId.trim();
        }
    }
}


