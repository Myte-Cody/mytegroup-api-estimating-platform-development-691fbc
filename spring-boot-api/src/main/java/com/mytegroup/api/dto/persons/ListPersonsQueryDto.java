package com.mytegroup.api.dto.persons;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ListPersonsQueryDto(
    String orgId,
    Boolean includeArchived,
    String personType,
    String companyId,
    String orgLocationId,
    String companyLocationId,
    String departmentKey,
    String skillKey,
    String search,
    String tag,
    @Min(value = 1, message = "Page must be at least 1")
    Integer page,
    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 100, message = "Limit must be at most 100")
    Integer limit
) {
    public ListPersonsQueryDto {
        if (orgId != null) {
            orgId = orgId.trim();
        }
        if (personType != null) {
            personType = personType.trim();
        }
        if (companyId != null) {
            companyId = companyId.trim();
        }
        if (orgLocationId != null) {
            orgLocationId = orgLocationId.trim();
        }
        if (companyLocationId != null) {
            companyLocationId = companyLocationId.trim();
        }
        if (departmentKey != null) {
            departmentKey = departmentKey.trim();
        }
        if (skillKey != null) {
            skillKey = skillKey.trim();
        }
        if (search != null) {
            search = search.trim();
        }
        if (tag != null) {
            tag = tag.trim();
        }
    }
}

