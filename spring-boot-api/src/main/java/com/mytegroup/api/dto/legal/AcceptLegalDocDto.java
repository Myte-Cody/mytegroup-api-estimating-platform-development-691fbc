package com.mytegroup.api.dto.legal;

import com.mytegroup.api.entity.enums.legal.LegalDocType;
import jakarta.validation.constraints.NotNull;

public record AcceptLegalDocDto(
    @NotNull(message = "Document type is required")
    LegalDocType docType,
    String version
) {
    public AcceptLegalDocDto {
        if (version != null) {
            version = version.trim();
        }
    }
}

