package com.mytegroup.api.dto.companylocations;

import jakarta.validation.constraints.Email;

import java.util.List;

public record UpdateCompanyLocationDto(
    String name,
    String externalId,
    String timezone,
    @Email(message = "Email must be valid")
    String email,
    String phone,
    String addressLine1,
    String addressLine2,
    String city,
    String region,
    String postal,
    String country,
    List<String> tagKeys,
    String notes
) {
    public UpdateCompanyLocationDto {
        if (name != null) {
            name = name.trim();
        }
        if (externalId != null) {
            externalId = externalId.trim();
        }
        if (timezone != null) {
            timezone = timezone.trim();
        }
        if (email != null) {
            email = email.toLowerCase().trim();
        }
        if (phone != null) {
            phone = phone.trim();
        }
        if (addressLine1 != null) {
            addressLine1 = addressLine1.trim();
        }
        if (addressLine2 != null) {
            addressLine2 = addressLine2.trim();
        }
        if (city != null) {
            city = city.trim();
        }
        if (region != null) {
            region = region.trim();
        }
        if (postal != null) {
            postal = postal.trim();
        }
        if (country != null) {
            country = country.trim();
        }
        if (notes != null) {
            notes = notes.trim();
        }
    }
}


