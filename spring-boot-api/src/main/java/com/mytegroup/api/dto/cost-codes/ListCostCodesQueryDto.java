package com.mytegroup.api.dto.costcodes;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ListCostCodesQueryDto(
    String q,
    String category,
    Boolean active,
    Boolean noPagination,
    String updatedSince,
    @Min(value = 1, message = "Page must be at least 1")
    Integer page,
    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 1000, message = "Limit must be at most 1000")
    Integer limit
) {
    public ListCostCodesQueryDto {
        if (q != null) {
            q = q.trim();
        }
        if (category != null) {
            category = category.trim();
        }
        if (updatedSince != null) {
            updatedSince = updatedSince.trim();
        }
    }
}


