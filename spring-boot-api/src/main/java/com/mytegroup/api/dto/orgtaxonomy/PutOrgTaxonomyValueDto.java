package com.mytegroup.api.dto.orgtaxonomy;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record PutOrgTaxonomyValueDto(
    @NotBlank(message = "Key is required")
    @Size(max = 255, message = "Key must be at most 255 characters")
    String key,
    @NotBlank(message = "Label is required")
    @Size(max = 255, message = "Label must be at most 255 characters")
    String label,
    @Min(value = 0, message = "Sort order must be at least 0")
    @Max(value = 9999, message = "Sort order must be at most 9999")
    Integer sortOrder,
    @Size(max = 50, message = "Color must be at most 50 characters")
    String color,
    Map<String, Object> metadata
) {
    public PutOrgTaxonomyValueDto {
        if (key != null) {
            key = key.trim();
        }
        if (label != null) {
            label = label.trim();
        }
        if (color != null) {
            color = color.trim();
        }
    }
}
