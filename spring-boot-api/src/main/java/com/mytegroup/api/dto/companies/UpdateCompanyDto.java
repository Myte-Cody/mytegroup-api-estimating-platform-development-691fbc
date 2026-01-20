package com.mytegroup.api.dto.companies;

import jakarta.validation.constraints.Email;

import java.util.List;

public record UpdateCompanyDto(
    String name,
    String externalId,
    String website,
    @Email(message = "Email must be valid")
    String mainEmail,
    String mainPhone,
    List<String> companyTypeKeys,
    List<String> tagKeys,
    Double rating,
    String notes
) {
    public UpdateCompanyDto {
        if (name != null) {
            name = name.trim();
        }
        if (externalId != null) {
            externalId = externalId.trim();
        }
        if (website != null) {
            website = website.trim();
        }
        if (mainEmail != null) {
            mainEmail = mainEmail.toLowerCase().trim();
        }
        if (mainPhone != null) {
            mainPhone = mainPhone.trim();
        }
        if (notes != null) {
            notes = notes.trim();
        }
    }
}

