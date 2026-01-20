package com.mytegroup.api.dto.email;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

public record SendEmailDto(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,
    @NotBlank(message = "Subject is required")
    String subject,
    String text,
    String body,
    String html,
    String templateName,
    String orgId,
    Map<String, Object> variables,
    String mode,
    List<@Email String> bcc
) {
    public SendEmailDto {
        if (email != null) {
            email = email.toLowerCase().trim();
        }
        if (subject != null) {
            subject = subject.trim();
        }
        if (templateName != null) {
            templateName = templateName.trim();
        }
        if (orgId != null) {
            orgId = orgId.trim();
        }
    }
}

