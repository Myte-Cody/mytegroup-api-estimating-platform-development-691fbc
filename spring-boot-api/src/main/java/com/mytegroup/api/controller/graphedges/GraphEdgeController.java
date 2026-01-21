package com.mytegroup.api.controller.graphedges;

import com.mytegroup.api.dto.graphedges.*;
import com.mytegroup.api.entity.core.GraphEdge;
import com.mytegroup.api.service.common.ActorContext;
import com.mytegroup.api.service.graphedges.GraphEdgesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/graph-edges")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class GraphEdgeController {

    private final GraphEdgesService graphEdgesService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public List<Map<String, Object>> list(
            @ModelAttribute ListGraphEdgesQueryDto query,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        List<GraphEdge> edges = graphEdgesService.list(
            query.getFromType(),
            query.getFromId(),
            query.getToType(),
            query.getToId(),
            query.getEdgeType(),
            actor,
            resolvedOrgId
        );
        
        return edges.stream()
            .map(this::edgeToResponse)
            .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public Map<String, Object> create(
            @RequestBody @Valid CreateGraphEdgeDto dto,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        GraphEdge edge = graphEdgesService.create(
            dto.getFromType(),
            dto.getFromId(),
            dto.getToType(),
            dto.getToId(),
            dto.getEdgeType(),
            dto.getMeta(),
            actor,
            resolvedOrgId
        );
        
        return edgeToResponse(edge);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public void delete(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        graphEdgesService.delete(id, actor, resolvedOrgId);
    }
    
    private Map<String, Object> edgeToResponse(GraphEdge edge) {
        return Map.of(
            "id", edge.getId(),
            "fromType", edge.getFromType() != null ? edge.getFromType() : "",
            "fromId", edge.getFromId() != null ? edge.getFromId() : "",
            "toType", edge.getToType() != null ? edge.getToType() : "",
            "toId", edge.getToId() != null ? edge.getToId() : "",
            "edgeType", edge.getEdgeType() != null ? edge.getEdgeType() : "",
            "meta", edge.getMeta() != null ? edge.getMeta() : Map.of(),
            "createdAt", edge.getCreatedAt() != null ? edge.getCreatedAt().toString() : ""
        );
    }
    
    private ActorContext getActorContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return new ActorContext(null, null, null, null);
        }
        
        Long userId = null;
        if (auth.getPrincipal() instanceof Long) {
            userId = (Long) auth.getPrincipal();
        } else if (auth.getPrincipal() instanceof String) {
            try {
                userId = Long.parseLong((String) auth.getPrincipal());
            } catch (NumberFormatException ignored) {}
        }
        
        return new ActorContext(
            userId != null ? userId.toString() : null,
            null,
            null,
            null
        );
    }
}
