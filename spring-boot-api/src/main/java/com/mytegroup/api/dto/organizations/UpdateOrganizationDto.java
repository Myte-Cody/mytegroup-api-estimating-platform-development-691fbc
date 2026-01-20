package com.mytegroup.api.dto.organizations;

import java.util.Map;

public record UpdateOrganizationDto(
    String name,
    Map<String, Object> metadata
) {
    public UpdateOrganizationDto {
        if (name != null) {
            name = name.trim();
        }
    }
}

