package com.mytegroup.api.controller.graphedges;

import com.mytegroup.api.dto.graphedges.*;
import com.mytegroup.api.dto.response.GraphEdgeResponseDto;
import com.mytegroup.api.entity.organization.GraphEdge;
import com.mytegroup.api.mapper.graphedges.GraphEdgeMapper;
import com.mytegroup.api.mapper.response.GraphEdgeResponseMapper;
import com.mytegroup.api.service.graphedges.GraphEdgesService;
import com.mytegroup.api.service.common.ServiceAuthorizationHelper;
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
    private final GraphEdgeMapper graphEdgeMapper;
    private final GraphEdgeResponseMapper graphEdgeResponseMapper;
    private final ServiceAuthorizationHelper authHelper;

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public List<GraphEdgeResponseDto> list(
            @ModelAttribute ListGraphEdgesQueryDto query,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            throw new IllegalArgumentException("orgId is required");
        }
        List<GraphEdge> edges = graphEdgesService.list(
            orgId,
            query.getEdgeType()
        );
        
        return edges.stream()
            .map(graphEdgeResponseMapper::toDto)
            .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public GraphEdgeResponseDto create(
            @RequestBody @Valid CreateGraphEdgeDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            throw new IllegalArgumentException("orgId is required");
        }
        // Use mapper to convert DTO to entity
        com.mytegroup.api.entity.core.Organization org = authHelper.validateOrg(orgId);
        GraphEdge edge = graphEdgeMapper.toEntity(dto, org);
        GraphEdge createdEdge = graphEdgesService.create(edge, orgId);
        
        return graphEdgeResponseMapper.toDto(createdEdge);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public void delete(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            throw new IllegalArgumentException("orgId is required");
        }
        graphEdgesService.delete(id, orgId);
    }
    
}
