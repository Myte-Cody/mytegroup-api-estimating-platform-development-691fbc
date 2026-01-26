package com.mytegroup.api.dto.people;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record ContactCertificationDto(
    @NotBlank(message = "Certification name is required")
    String name,
    LocalDate issuedAt,
    LocalDate expiresAt,
    String documentUrl,
    String notes
) {
    public ContactCertificationDto {
        if (name != null) {
            name = name.trim();
        }
        if (documentUrl != null) {
            documentUrl = documentUrl.trim();
        }
        if (notes != null) {
            notes = notes.trim();
        }
    }
}



