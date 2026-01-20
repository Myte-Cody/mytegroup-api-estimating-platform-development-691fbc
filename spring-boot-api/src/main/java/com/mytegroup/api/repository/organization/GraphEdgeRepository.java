package com.mytegroup.api.repository.organization;

import com.mytegroup.api.entity.enums.organization.GraphNodeType;
import com.mytegroup.api.entity.organization.GraphEdge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GraphEdgeRepository extends JpaRepository<GraphEdge, Long> {

    // Find by from node
    List<GraphEdge> findByOrgIdAndFromNodeTypeAndFromNodeId(Long orgId, GraphNodeType nodeType, Long nodeId);

    // Find by to node
    List<GraphEdge> findByOrgIdAndToNodeTypeAndToNodeId(Long orgId, GraphNodeType nodeType, Long nodeId);

    // Find by edge type key
    List<GraphEdge> findByOrgIdAndEdgeTypeKey(Long orgId, String edgeTypeKey);

    // List active edges
    List<GraphEdge> findByOrgIdAndArchivedAtIsNull(Long orgId);

    // Find all for org (including archived)
    List<GraphEdge> findByOrgId(Long orgId);
}

