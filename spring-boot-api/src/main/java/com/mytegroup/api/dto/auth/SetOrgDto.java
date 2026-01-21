package com.mytegroup.api.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SetOrgDto {
    
    @NotBlank(message = "Organization ID is required")
    private String orgId;
}
