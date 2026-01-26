package com.mytegroup.api.dto.costcodes;

import jakarta.validation.constraints.NotBlank;

public record CreateCostCodeDto(
    @NotBlank(message = "Category is required")
    String category,
    @NotBlank(message = "Code is required")
    String code,
    @NotBlank(message = "Description is required")
    String description
) {
    public CreateCostCodeDto {
        if (category != null) {
            category = category.trim();
        }
        if (code != null) {
            code = code.trim();
        }
        if (description != null) {
            description = description.trim();
        }
    }
}



