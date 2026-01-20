package com.mytegroup.api.dto.graphedges;

import com.mytegroup.api.entity.enums.organization.GraphNodeType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ListGraphEdgesQueryDto(
    String orgId,
    Boolean includeArchived,
    GraphNodeType fromNodeType,
    String fromNodeId,
    GraphNodeType toNodeType,
    String toNodeId,
    String edgeTypeKey,
    @Min(value = 1, message = "Page must be at least 1")
    Integer page,
    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 100, message = "Limit must be at most 100")
    Integer limit
) {
    public ListGraphEdgesQueryDto {
        if (orgId != null) {
            orgId = orgId.trim();
        }
        if (fromNodeId != null) {
            fromNodeId = fromNodeId.trim();
        }
        if (toNodeId != null) {
            toNodeId = toNodeId.trim();
        }
        if (edgeTypeKey != null) {
            edgeTypeKey = edgeTypeKey.trim().toLowerCase().replaceAll("[^a-z0-9_]", "_");
        }
    }
}

