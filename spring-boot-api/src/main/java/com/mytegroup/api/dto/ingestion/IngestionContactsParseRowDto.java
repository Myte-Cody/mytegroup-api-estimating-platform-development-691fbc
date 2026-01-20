package com.mytegroup.api.dto.ingestion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record IngestionContactsParseRowDto(
    @NotBlank(message = "Profile is required")
    String profile,
    @NotNull(message = "Cells are required")
    Map<String, Object> cells,
    Boolean allowAiProcessing
) {
    public IngestionContactsParseRowDto {
        if (profile != null) {
            profile = profile.trim();
        }
    }
}

