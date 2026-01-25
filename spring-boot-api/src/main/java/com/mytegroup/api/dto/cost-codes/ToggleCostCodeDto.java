package com.mytegroup.api.dto.costcodes;

import jakarta.validation.constraints.NotNull;

public record ToggleCostCodeDto(
    @NotNull(message = "Active status is required")
    Boolean active
) {
}


