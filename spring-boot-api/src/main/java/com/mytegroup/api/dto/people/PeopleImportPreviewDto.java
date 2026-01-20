package com.mytegroup.api.dto.people;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PeopleImportPreviewDto(
    @NotNull(message = "Rows are required")
    @NotEmpty(message = "At least one row is required")
    @Size(max = 1000, message = "Rows must not exceed 1000 items")
    @Valid
    List<PeopleImportRowDto> rows
) {
}

