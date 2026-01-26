package com.mytegroup.api.dto.emailtemplates;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record TestSendTemplateDto(
    @NotBlank(message = "To email is required")
    @Email(message = "Email must be valid")
    String to,
    String locale,
    Map<String, Object> variables
) {
    public TestSendTemplateDto {
        if (to != null) {
            to = to.toLowerCase().trim();
        }
        if (locale != null) {
            locale = locale.trim();
        }
    }
}



