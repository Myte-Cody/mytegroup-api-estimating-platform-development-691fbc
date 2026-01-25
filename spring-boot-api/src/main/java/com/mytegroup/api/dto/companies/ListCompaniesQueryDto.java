package com.mytegroup.api.dto.companies;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ListCompaniesQueryDto(
    String orgId,
    Boolean includeArchived,
    Boolean includeCounts,
    String search,
    String type,
    String tag,
    @Min(value = 1, message = "Page must be at least 1")
    Integer page,
    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 100, message = "Limit must be at most 100")
    Integer limit
) {
    public ListCompaniesQueryDto {
        if (orgId != null) {
            orgId = orgId.trim();
        }
        if (search != null) {
            search = search.trim();
        }
        if (type != null) {
            type = type.trim();
        }
        if (tag != null) {
            tag = tag.trim();
        }
    }
}


