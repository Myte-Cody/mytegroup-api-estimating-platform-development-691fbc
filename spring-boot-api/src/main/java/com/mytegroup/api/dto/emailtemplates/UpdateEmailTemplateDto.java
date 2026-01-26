package com.mytegroup.api.dto.emailtemplates;

import jakarta.validation.constraints.NotBlank;

public record UpdateEmailTemplateDto(
    String locale,
    @NotBlank(message = "Subject is required")
    String subject,
    @NotBlank(message = "HTML content is required")
    String html,
    @NotBlank(message = "Text content is required")
    String text
) {
    public UpdateEmailTemplateDto {
        if (locale != null) {
            locale = locale.trim();
        }
        if (subject != null) {
            subject = subject.trim();
        }
    }
}



