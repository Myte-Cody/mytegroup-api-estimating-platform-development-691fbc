package com.mytegroup.api.mapper.graphedges;

import com.mytegroup.api.dto.graphedges.CreateGraphEdgeDto;
import com.mytegroup.api.entity.organization.GraphEdge;
import com.mytegroup.api.entity.core.Organization;
import org.springframework.stereotype.Component;

@Component
public class GraphEdgeMapper {
    public GraphEdge toEntity(CreateGraphEdgeDto dto, Organization organization) {
        GraphEdge edge = new GraphEdge();
        edge.setOrganization(organization);
        edge.setFromNodeType(dto.fromNodeType());
        edge.setFromNodeId(dto.fromNodeId());
        edge.setToNodeType(dto.toNodeType());
        edge.setToNodeId(dto.toNodeId());
        edge.setEdgeTypeKey(dto.edgeTypeKey());
        edge.setMetadata(dto.metadata());
        edge.setEffectiveFrom(dto.effectiveFrom());
        edge.setEffectiveTo(dto.effectiveTo());
        return edge;
    }
}

