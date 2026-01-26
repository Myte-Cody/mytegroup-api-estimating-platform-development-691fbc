package com.mytegroup.api.dto.companies;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CompaniesImportConfirmRowDto(
    @NotNull(message = "Row number is required")
    @Min(value = 1, message = "Row number must be at least 1")
    Integer row,
    @NotBlank(message = "Company name is required")
    String companyName,
    @Size(max = 50, message = "Company type keys must not exceed 50 items")
    List<String> companyTypeKeys,
    @Size(max = 50, message = "Company tag keys must not exceed 50 items")
    List<String> companyTagKeys,
    String companyExternalId,
    String website,
    @Email(message = "Email must be valid")
    String mainEmail,
    String mainPhone,
    String notes,
    String locationName,
    String locationExternalId,
    @Email(message = "Email must be valid")
    String locationEmail,
    String locationPhone,
    String locationAddressLine1,
    String locationCity,
    String locationRegion,
    String locationPostal,
    String locationCountry,
    @Size(max = 50, message = "Location tag keys must not exceed 50 items")
    List<String> locationTagKeys,
    String locationNotes,
    @NotNull(message = "Action is required")
    String action
) {
    public CompaniesImportConfirmRowDto {
        if (companyName != null) {
            companyName = companyName.trim();
        }
        if (companyExternalId != null) {
            companyExternalId = companyExternalId.trim();
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
        if (locationName != null) {
            locationName = locationName.trim();
        }
        if (locationExternalId != null) {
            locationExternalId = locationExternalId.trim();
        }
        if (locationEmail != null) {
            locationEmail = locationEmail.toLowerCase().trim();
        }
        if (locationPhone != null) {
            locationPhone = locationPhone.trim();
        }
        if (locationAddressLine1 != null) {
            locationAddressLine1 = locationAddressLine1.trim();
        }
        if (locationCity != null) {
            locationCity = locationCity.trim();
        }
        if (locationRegion != null) {
            locationRegion = locationRegion.trim();
        }
        if (locationPostal != null) {
            locationPostal = locationPostal.trim();
        }
        if (locationCountry != null) {
            locationCountry = locationCountry.trim();
        }
        if (locationNotes != null) {
            locationNotes = locationNotes.trim();
        }
        if (action != null) {
            action = action.trim().toLowerCase();
        }
    }
}



