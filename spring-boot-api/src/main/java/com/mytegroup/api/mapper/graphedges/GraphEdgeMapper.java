package com.mytegroup.api.mapper.graphedges;

import com.mytegroup.api.dto.graphedges.CreateGraphEdgeDto;
import com.mytegroup.api.entity.organization.GraphEdge;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.enums.organization.GraphNodeType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class GraphEdgeMapper {
    public GraphEdge toEntity(CreateGraphEdgeDto dto, Organization organization) {
        GraphEdge edge = new GraphEdge();
        edge.setOrganization(organization);
        
        // Convert String to GraphNodeType enum
        if (dto.getFromType() != null) {
            edge.setFromNodeType(GraphNodeType.valueOf(dto.getFromType().toUpperCase()));
        }
        if (dto.getToType() != null) {
            edge.setToNodeType(GraphNodeType.valueOf(dto.getToType().toUpperCase()));
        }
        
        // Convert String to Long
        if (dto.getFromId() != null) {
            edge.setFromNodeId(Long.parseLong(dto.getFromId()));
        }
        if (dto.getToId() != null) {
            edge.setToNodeId(Long.parseLong(dto.getToId()));
        }
        
        edge.setEdgeTypeKey(dto.getEdgeType());
        edge.setMetadata(dto.getMeta());
        
        // Convert LocalDate to LocalDateTime
        if (dto.getEffectiveFrom() != null) {
            edge.setEffectiveFrom(dto.getEffectiveFrom().atStartOfDay());
        }
        if (dto.getEffectiveTo() != null) {
            edge.setEffectiveTo(dto.getEffectiveTo().atTime(23, 59, 59));
        }
        
        return edge;
    }
}

