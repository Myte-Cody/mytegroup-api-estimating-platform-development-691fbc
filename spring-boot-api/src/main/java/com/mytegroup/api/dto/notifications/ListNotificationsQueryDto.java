package com.mytegroup.api.dto.notifications;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ListNotificationsQueryDto(
    Boolean read,
    @Min(value = 1, message = "Page must be at least 1")
    Integer page,
    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 100, message = "Limit must be at most 100")
    Integer limit
) {
}

