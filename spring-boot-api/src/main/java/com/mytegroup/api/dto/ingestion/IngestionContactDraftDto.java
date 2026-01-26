package com.mytegroup.api.dto.ingestion;

import jakarta.validation.Valid;

public record IngestionContactDraftDto(
    @Valid
    IngestionPersonDraftDto person,
    String companyName
) {
    public IngestionContactDraftDto {
        if (companyName != null) {
            companyName = companyName.trim();
        }
    }
}



