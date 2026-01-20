package com.mytegroup.api.dto.events;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDateTime;

public record ListEventsDto(
    String entityType,
    String entityId,
    String actor,
    String action,
    String eventType,
    String cursor,
    Boolean archived,
    LocalDateTime createdAtGte,
    LocalDateTime createdAtLte,
    @Min(value = 0, message = "Offset must be non-negative")
    Integer offset,
    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 100, message = "Limit must be at most 100")
    Integer limit,
    String orgId,
    String sort
) {
    public ListEventsDto {
        if (entityType != null) {
            entityType = entityType.trim();
        }
        if (entityId != null) {
            entityId = entityId.trim();
        }
        if (actor != null) {
            actor = actor.trim();
        }
        if (action != null) {
            action = action.trim();
        }
        if (eventType != null) {
            eventType = eventType.trim();
        }
        if (cursor != null) {
            cursor = cursor.trim();
        }
        if (orgId != null) {
            orgId = orgId.trim();
        }
        if (sort != null) {
            sort = sort.trim().toLowerCase();
        }
    }
}

