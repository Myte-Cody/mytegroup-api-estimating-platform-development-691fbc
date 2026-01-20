package com.mytegroup.api.dto.invites;

import com.mytegroup.api.common.enums.Role;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateInviteDto(
    @NotBlank(message = "Person ID is required")
    String personId,
    @NotNull(message = "Role is required")
    Role role,
    @Min(value = 1, message = "Expires in hours must be at least 1")
    @Max(value = 168, message = "Expires in hours must be at most 168")
    Integer expiresInHours
) {
}

