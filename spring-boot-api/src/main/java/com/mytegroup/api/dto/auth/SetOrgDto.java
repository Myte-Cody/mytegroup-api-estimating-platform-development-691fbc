package com.mytegroup.api.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record SetOrgDto(
    @NotBlank(message = "Organization ID is required")
    String orgId
) {
    public SetOrgDto {
        if (orgId != null) {
            orgId = orgId.trim();
        }
    }
}

