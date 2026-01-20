package com.mytegroup.api.dto.organizations;

import jakarta.validation.constraints.NotNull;

public record UpdateOrganizationLegalHoldDto(
    @NotNull(message = "Legal hold status is required")
    Boolean legalHold
) {
}

