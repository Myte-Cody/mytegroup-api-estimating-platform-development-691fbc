package com.mytegroup.api.dto.users;

import com.mytegroup.api.common.enums.Role;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRolesDto {
    
    @NotNull(message = "Roles are required")
    @NotEmpty(message = "At least one role is required")
    private List<Role> roles;
}
