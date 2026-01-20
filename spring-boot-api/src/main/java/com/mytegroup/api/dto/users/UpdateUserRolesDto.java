package com.mytegroup.api.dto.users;

import com.mytegroup.api.common.enums.Role;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateUserRolesDto(
    @NotNull(message = "Roles are required")
    @NotEmpty(message = "At least one role is required")
    List<Role> roles
) {
}

