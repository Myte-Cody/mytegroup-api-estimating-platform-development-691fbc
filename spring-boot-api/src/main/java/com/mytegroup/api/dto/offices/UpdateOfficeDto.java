package com.mytegroup.api.dto.offices;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateOfficeDto(
    String name,
    String address,
    String description,
    String timezone,
    String orgLocationTypeKey,
    @Size(max = 100, message = "Tag keys must not exceed 100 items")
    List<String> tagKeys,
    String parentOrgLocationId,
    @Min(value = 0, message = "Sort order must be non-negative")
    Integer sortOrder
) {
    public UpdateOfficeDto {
        if (name != null) {
            name = name.trim();
        }
        if (address != null) {
            address = address.trim();
        }
        if (description != null) {
            description = description.trim();
        }
        if (timezone != null) {
            timezone = timezone.trim();
        }
        if (orgLocationTypeKey != null) {
            orgLocationTypeKey = orgLocationTypeKey.trim();
        }
        if (parentOrgLocationId != null) {
            parentOrgLocationId = parentOrgLocationId.trim();
        }
    }
}

