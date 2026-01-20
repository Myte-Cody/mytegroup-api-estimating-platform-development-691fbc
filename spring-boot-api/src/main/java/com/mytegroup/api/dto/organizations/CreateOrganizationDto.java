package com.mytegroup.api.dto.organizations;

import com.mytegroup.api.entity.enums.organization.DataResidency;
import com.mytegroup.api.entity.enums.organization.DatastoreType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record CreateOrganizationDto(
    @NotBlank(message = "Name is required")
    String name,
    Map<String, Object> metadata,
    String databaseUri,
    String datastoreUri,
    String databaseName,
    @Size(max = 120, message = "Primary domain must be at most 120 characters")
    String primaryDomain,
    Boolean useDedicatedDb,
    DatastoreType datastoreType,
    DataResidency dataResidency,
    Boolean piiStripped,
    Boolean legalHold
) {
    public CreateOrganizationDto {
        if (name != null) {
            name = name.trim();
        }
        if (primaryDomain != null) {
            primaryDomain = primaryDomain.trim();
        }
    }
}

