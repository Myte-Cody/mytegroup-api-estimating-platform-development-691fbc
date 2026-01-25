package com.mytegroup.api.dto.companylocations;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ListCompanyLocationsQueryDto(
    String orgId,
    Boolean includeArchived,
    String companyId,
    String search,
    String tag,
    @Min(value = 1, message = "Page must be at least 1")
    Integer page,
    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 100, message = "Limit must be at most 100")
    Integer limit
) {
    public ListCompanyLocationsQueryDto {
        if (orgId != null) {
            orgId = orgId.trim();
        }
        if (companyId != null) {
            companyId = companyId.trim();
        }
        if (search != null) {
            search = search.trim();
        }
        if (tag != null) {
            tag = tag.trim();
        }
    }
}


