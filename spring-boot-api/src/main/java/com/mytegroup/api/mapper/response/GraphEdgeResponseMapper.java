package com.mytegroup.api.mapper.response;

import com.mytegroup.api.dto.response.GraphEdgeResponseDto;
import com.mytegroup.api.entity.organization.GraphEdge;
import org.springframework.stereotype.Component;

@Component
public class GraphEdgeResponseMapper {
    public GraphEdgeResponseDto toDto(GraphEdge entity) {
        if (entity == null) {
            return null;
        }
        
        return GraphEdgeResponseDto.builder()
                .id(entity.getId())
                .fromNodeType(entity.getFromNodeType() != null ? entity.getFromNodeType().getValue() : null)
                .fromNodeId(entity.getFromNodeId())
                .toNodeType(entity.getToNodeType() != null ? entity.getToNodeType().getValue() : null)
                .toNodeId(entity.getToNodeId())
                .edgeTypeKey(entity.getEdgeTypeKey())
                .metadata(entity.getMetadata())
                .effectiveFrom(entity.getEffectiveFrom())
                .effectiveTo(entity.getEffectiveTo())
                .orgId(entity.getOrganization() != null ? entity.getOrganization().getId().toString() : null)
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

