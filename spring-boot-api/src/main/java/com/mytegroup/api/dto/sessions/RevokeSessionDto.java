package com.mytegroup.api.dto.sessions;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record RevokeSessionDto(
    @NotBlank(message = "Session ID is required")
    @Length(min = 5, max = 200, message = "Session ID must be between 5 and 200 characters")
    String sessionId
) {
}

