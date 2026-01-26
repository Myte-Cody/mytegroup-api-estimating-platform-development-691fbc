package com.mytegroup.api.dto.organizations;

import com.mytegroup.api.entity.enums.organization.DatastoreType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ListOrganizationsDto(
    String search,
    Boolean includeArchived,
    DatastoreType datastoreType,
    Boolean legalHold,
    Boolean piiStripped,
    @Min(value = 1, message = "Page must be at least 1")
    Integer page,
    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 100, message = "Limit must be at most 100")
    Integer limit
) {
    public ListOrganizationsDto {
        if (search != null) {
            search = search.trim();
        }
    }
}



