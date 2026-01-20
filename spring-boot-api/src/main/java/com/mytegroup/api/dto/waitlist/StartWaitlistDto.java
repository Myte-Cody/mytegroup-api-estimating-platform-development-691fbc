package com.mytegroup.api.dto.waitlist;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.dto.common.ValidationConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StartWaitlistDto(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 120, message = "Name must be between 2 and 120 characters")
    String name,
    @Pattern(regexp = ValidationConstants.E164_PHONE_REGEX, message = ValidationConstants.PHONE_REGEX_MESSAGE)
    String phone,
    Role role,
    @Size(max = 120, message = "Source must be at most 120 characters")
    String source,
    Boolean preCreateAccount,
    Boolean marketingConsent,
    String trap
) {
    public StartWaitlistDto {
        if (email != null) {
            email = email.toLowerCase().trim();
        }
        if (name != null) {
            name = name.trim();
        }
        if (phone != null) {
            phone = phone.trim();
        }
        if (source != null) {
            source = source.trim();
        }
        if (trap != null) {
            trap = trap.trim();
        }
    }
}
