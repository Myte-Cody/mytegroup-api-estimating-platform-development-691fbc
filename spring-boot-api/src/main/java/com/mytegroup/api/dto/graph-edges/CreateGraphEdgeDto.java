package com.mytegroup.api.dto.graphedges;

import com.mytegroup.api.entity.enums.organization.GraphNodeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Map;

public record CreateGraphEdgeDto(
    @NotNull(message = "From node type is required")
    GraphNodeType fromNodeType,
    @NotBlank(message = "From node ID is required")
    String fromNodeId,
    @NotNull(message = "To node type is required")
    GraphNodeType toNodeType,
    @NotBlank(message = "To node ID is required")
    String toNodeId,
    @NotBlank(message = "Edge type key is required")
    String edgeTypeKey,
    Map<String, Object> metadata,
    LocalDate effectiveFrom,
    LocalDate effectiveTo
) {
    public CreateGraphEdgeDto {
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

