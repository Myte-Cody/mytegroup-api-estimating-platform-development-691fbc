package com.mytegroup.api.dto.orgtaxonomy;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PutOrgTaxonomyDto(
    @NotNull(message = "Values are required")
    @NotEmpty(message = "At least one value is required")
    @Size(max = 1000, message = "Values must not exceed 1000 items")
    @Valid
    List<PutOrgTaxonomyValueDto> values
) {
}
