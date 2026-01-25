package com.mytegroup.api.dto.costcodes;

public record UpdateCostCodeDto(
    String category,
    String code,
    String description
) {
    public UpdateCostCodeDto {
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


