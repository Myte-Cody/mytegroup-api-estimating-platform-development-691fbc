package com.mytegroup.api.dto.ingestion;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record IngestionContactsSuggestMappingDto(
    String profile,
    @NotNull(message = "Headers are required")
    @NotEmpty(message = "At least one header is required")
    @Size(max = 200, message = "Headers must not exceed 200 items")
    List<String> headers,
    Boolean allowAiProcessing
) {
    public IngestionContactsSuggestMappingDto {
        if (profile != null) {
            profile = profile.trim();
        }
    }
}

