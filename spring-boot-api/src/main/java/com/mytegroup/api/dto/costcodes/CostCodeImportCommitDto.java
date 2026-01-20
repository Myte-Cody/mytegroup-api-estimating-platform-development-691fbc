package com.mytegroup.api.dto.costcodes;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CostCodeImportCommitDto(
    @Size(max = 1000, message = "Codes must not exceed 1000 items")
    @Valid
    List<CostCodeInputDto> codes
) {
}

