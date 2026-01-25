package com.mytegroup.api.dto.costcodes;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BulkCostCodesDto(
    @NotNull(message = "Codes are required")
    @NotEmpty(message = "At least one code is required")
    @Valid
    List<CostCodeInputDto> codes
) {
}


