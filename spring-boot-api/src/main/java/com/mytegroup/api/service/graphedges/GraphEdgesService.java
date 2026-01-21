package com.mytegroup.api.service.graphedges;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.enums.organization.GraphNodeType;
import com.mytegroup.api.entity.organization.GraphEdge;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.exception.ForbiddenException;
import com.mytegroup.api.exception.ResourceNotFoundException;
import com.mytegroup.api.repository.organization.GraphEdgeRepository;
import com.mytegroup.api.service.common.ActorContext;
import com.mytegroup.api.service.common.AuditLogService;
import com.mytegroup.api.service.common.ServiceAuthorizationHelper;
import com.mytegroup.api.service.common.ServiceValidationHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for graph edge management.
 * Handles CRUD operations for relationship edges between entities.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GraphEdgesService {
    
    private final GraphEdgeRepository graphEdgeRepository;
    private final AuditLogService auditLogService;
    private final ServiceAuthorizationHelper authHelper;
    private final ServiceValidationHelper validationHelper;
    
    /**
     * Creates a new graph edge
     */
    @Transactional
    public GraphEdge create(GraphEdge edge, ActorContext actor, String orgId) {
        authHelper.ensureRole(actor, Role.ORG_OWNER, Role.ORG_ADMIN, Role.ADMIN, Role.SUPER_ADMIN, Role.PLATFORM_ADMIN);
        authHelper.ensureOrgScope(orgId, actor);
        Organization org = authHelper.validateOrg(orgId);
        edge.setOrganization(org);
        
        // Validate edge type key
        String edgeTypeKey = validationHelper.normalizeKey(edge.getEdgeTypeKey());
        if (edgeTypeKey == null || edgeTypeKey.isEmpty()) {
            throw new BadRequestException("Edge type key is required");
        }
        edge.setEdgeTypeKey(edgeTypeKey);
        
        // TODO: Validate node types and edge rules
        
        GraphEdge savedEdge = graphEdgeRepository.save(edge);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("edgeTypeKey", savedEdge.getEdgeTypeKey());
        metadata.put("fromNodeType", savedEdge.getFromNodeType() != null ? savedEdge.getFromNodeType().toString() : null);
        metadata.put("toNodeType", savedEdge.getToNodeType() != null ? savedEdge.getToNodeType().toString() : null);
        
        auditLogService.log(
            "graph_edge.created",
            orgId,
            actor != null ? actor.getUserId() : null,
            "GraphEdge",
            savedEdge.getId().toString(),
            metadata
        );
        
        return savedEdge;
    }
    
    /**
     * Lists graph edges for an organization
     */
    @Transactional(readOnly = true)
    public List<GraphEdge> list(ActorContext actor, String orgId, String edgeTypeKey, 
                                 GraphNodeType fromNodeType, Long fromNodeId,
                                 GraphNodeType toNodeType, Long toNodeId) {
        authHelper.ensureRole(actor, Role.ORG_OWNER, Role.ORG_ADMIN, Role.ADMIN, Role.SUPER_ADMIN, Role.PLATFORM_ADMIN);
        authHelper.ensureOrgScope(orgId, actor);
        
        Long orgIdLong = Long.parseLong(orgId);
        
        if (fromNodeType != null && fromNodeId != null) {
            return graphEdgeRepository.findByOrgIdAndFromNodeTypeAndFromNodeId(orgIdLong, fromNodeType, fromNodeId);
        }
        if (toNodeType != null && toNodeId != null) {
            return graphEdgeRepository.findByOrgIdAndToNodeTypeAndToNodeId(orgIdLong, toNodeType, toNodeId);
        }
        if (edgeTypeKey != null) {
            return graphEdgeRepository.findByOrgIdAndEdgeTypeKey(orgIdLong, edgeTypeKey);
        }
        
        return graphEdgeRepository.findByOrgIdAndArchivedAtIsNull(orgIdLong);
    }
    
    /**
     * Deletes a graph edge
     */
    @Transactional
    public void delete(Long id, ActorContext actor, String orgId) {
        authHelper.ensureRole(actor, Role.ORG_OWNER, Role.ORG_ADMIN, Role.ADMIN, Role.SUPER_ADMIN, Role.PLATFORM_ADMIN);
        authHelper.ensureOrgScope(orgId, actor);
        
        GraphEdge edge = graphEdgeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Graph edge not found"));
        
        if (edge.getOrganization() == null || 
            !edge.getOrganization().getId().toString().equals(orgId)) {
            throw new ForbiddenException("Cannot access graph edge outside your organization");
        }
        
        edge.setArchivedAt(LocalDateTime.now());
        graphEdgeRepository.save(edge);
        
        auditLogService.log(
            "graph_edge.deleted",
            orgId,
            actor != null ? actor.getUserId() : null,
            "GraphEdge",
            edge.getId().toString(),
            null
        );
    }
}

