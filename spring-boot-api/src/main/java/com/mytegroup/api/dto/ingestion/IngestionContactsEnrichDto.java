package com.mytegroup.api.dto.ingestion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record IngestionContactsEnrichDto(
    @NotBlank(message = "Profile is required")
    String profile,
    @NotNull(message = "Candidate is required")
    @Valid
    IngestionContactDraftDto candidate,
    Boolean allowAiProcessing
) {
    public IngestionContactsEnrichDto {
        if (profile != null) {
            profile = profile.trim();
        }
    }
}

