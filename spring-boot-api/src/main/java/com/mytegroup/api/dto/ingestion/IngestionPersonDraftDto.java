package com.mytegroup.api.dto.ingestion;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;

import java.util.List;

public record IngestionPersonDraftDto(
    String displayName,
    @Size(max = 10, message = "Emails must not exceed 10 items")
    @Email(message = "Email must be valid")
    List<@Email String> emails,
    @Size(max = 10, message = "Phones must not exceed 10 items")
    List<String> phones
) {
    public IngestionPersonDraftDto {
        if (displayName != null) {
            displayName = displayName.trim();
        }
    }
}

