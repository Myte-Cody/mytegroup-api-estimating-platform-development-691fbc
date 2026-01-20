package com.mytegroup.api.dto.contactinquiries;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyContactInquiryDto(
    @Size(min = 2, max = 120, message = "Name must be between 2 and 120 characters")
    String name,
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,
    String trap
) {
    public VerifyContactInquiryDto {
        if (name != null) {
            name = name.trim();
        }
        if (email != null) {
            email = email.toLowerCase().trim();
        }
    }
}

