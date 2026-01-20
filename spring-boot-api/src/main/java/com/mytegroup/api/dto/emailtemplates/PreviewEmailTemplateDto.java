package com.mytegroup.api.dto.emailtemplates;

import java.util.Map;

public record PreviewEmailTemplateDto(
    String locale,
    Map<String, Object> variables
) {
    public PreviewEmailTemplateDto {
        if (locale != null) {
            locale = locale.trim();
        }
    }
}

