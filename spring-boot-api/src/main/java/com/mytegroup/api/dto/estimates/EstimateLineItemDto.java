package com.mytegroup.api.dto.estimates;

import jakarta.validation.constraints.Min;

public record EstimateLineItemDto(
    String code,
    String description,
    @Min(value = 0, message = "Quantity must be non-negative")
    Double quantity,
    String unit,
    @Min(value = 0, message = "Unit cost must be non-negative")
    Double unitCost,
    @Min(value = 0, message = "Total must be non-negative")
    Double total
) {
    public EstimateLineItemDto {
        if (code != null) {
            code = code.trim();
        }
        if (description != null) {
            description = description.trim();
        }
        if (unit != null) {
            unit = unit.trim();
        }
    }
}


