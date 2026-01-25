package com.mytegroup.api.dto.estimates;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateEstimateDto(
    @NotBlank(message = "Name is required")
    String name,
    String description,
    String notes,
    @Valid
    List<EstimateLineItemDto> lineItems
) {
    public CreateEstimateDto {
        if (name != null) {
            name = name.trim();
        }
        if (description != null) {
            description = description.trim();
        }
        if (notes != null) {
            notes = notes.trim();
        }
    }
}


