package com.mytegroup.api.dto.contactinquiries;

import com.mytegroup.api.dto.common.ValidationConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ConfirmContactInquiryDto(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,
    @NotBlank(message = "Code is required")
    @Pattern(regexp = ValidationConstants.VERIFICATION_CODE_REGEX, message = "Code must be 6 digits")
    String code
) {
    public ConfirmContactInquiryDto {
        if (email != null) {
            email = email.toLowerCase().trim();
        }
    }
}

