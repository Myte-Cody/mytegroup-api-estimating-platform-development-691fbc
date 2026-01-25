package com.mytegroup.api.dto.organizations;

import jakarta.validation.constraints.NotNull;

public record UpdateOrganizationPiiDto(
    @NotNull(message = "PII stripped status is required")
    Boolean piiStripped
) {
}


