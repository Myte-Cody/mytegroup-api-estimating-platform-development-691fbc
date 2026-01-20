package com.mytegroup.api.dto.legal;

import com.mytegroup.api.entity.enums.legal.LegalDocType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateLegalDocDto(
    @NotNull(message = "Type is required")
    LegalDocType type,
    @NotBlank(message = "Version is required")
    @Size(min = 1, message = "Version must be at least 1 character")
    String version,
    @NotBlank(message = "Content is required")
    @Size(min = 10, message = "Content must be at least 10 characters")
    String content,
    LocalDateTime effectiveAt
) {
    public CreateLegalDocDto {
        if (version != null) {
            version = version.trim();
        }
        if (content != null) {
            content = content.trim();
        }
    }
}

