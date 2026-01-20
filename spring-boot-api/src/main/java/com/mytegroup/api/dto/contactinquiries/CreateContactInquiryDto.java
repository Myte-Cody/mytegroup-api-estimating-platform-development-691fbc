package com.mytegroup.api.dto.contactinquiries;

import com.mytegroup.api.entity.enums.communication.ContactInquiryStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateContactInquiryDto(
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 120, message = "Name must be between 2 and 120 characters")
    String name,
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,
    @NotBlank(message = "Message is required")
    @Size(min = 10, max = 4000, message = "Message must be between 10 and 4000 characters")
    String message,
    @Size(max = 120, message = "Source must be at most 120 characters")
    String source,
    String trap
) {
    public CreateContactInquiryDto {
        if (name != null) {
            name = name.trim();
        }
        if (email != null) {
            email = email.toLowerCase().trim();
        }
        if (message != null) {
            message = message.trim();
        }
        if (source != null) {
            source = source.trim();
        }
    }
}

