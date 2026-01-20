package com.mytegroup.api.dto.estimates;

import com.mytegroup.api.entity.enums.projects.EstimateStatus;
import jakarta.validation.Valid;

import java.util.List;

public record UpdateEstimateDto(
    String name,
    String description,
    String notes,
    EstimateStatus status,
    @Valid
    List<EstimateLineItemDto> lineItems
) {
    public UpdateEstimateDto {
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

