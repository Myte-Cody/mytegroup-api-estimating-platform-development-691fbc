package com.mytegroup.api.controller.estimates;

import com.mytegroup.api.dto.estimates.*;
import com.mytegroup.api.dto.response.EstimateResponseDto;
import com.mytegroup.api.entity.projects.Estimate;
import com.mytegroup.api.mapper.estimates.EstimateMapper;
import com.mytegroup.api.mapper.estimates.EstimateMapper;
import com.mytegroup.api.service.estimates.EstimatesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Estimates controller (nested under projects).
 * Endpoints:
 * - GET /projects/:projectId/estimates - List estimates
 * - POST /projects/:projectId/estimates - Create estimate
 * - GET /projects/:projectId/estimates/:id - Get estimate
 * - PATCH /projects/:projectId/estimates/:id - Update estimate
 * - POST /projects/:projectId/estimates/:id/archive - Archive estimate
 * - POST /projects/:projectId/estimates/:id/unarchive - Unarchive estimate
 */
@RestController
@RequestMapping("/api/projects/{projectId}/estimates")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class EstimateController {

    private final EstimatesService estimatesService;
    private final EstimateMapper estimateMapper;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER', 'PM', 'VIEWER', 'ORG_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<List<EstimateResponseDto>> list(
            @PathVariable Long projectId,
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) Boolean includeArchived) {
        
        if (orgId == null) { 
            throw new IllegalArgumentException("orgId is required");
        }
        List<Estimate> estimates = estimatesService.listByProject(projectId, orgId, includeArchived);
        
        List<EstimateResponseDto> response = estimates.stream()
            .map(estimateMapper::toDto)
            .toList();
        
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER', 'ORG_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<EstimateResponseDto> create(
            @PathVariable Long projectId,
            @RequestBody @Valid CreateEstimateDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            throw new IllegalArgumentException("orgId is required");
        }
        Estimate estimate = estimateMapper.toEntity(dto, null, null, null);
        
        Estimate savedEstimate = estimatesService.create(estimate, projectId, orgId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(estimateMapper.toDto(savedEstimate));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER', 'PM', 'VIEWER', 'ORG_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<EstimateResponseDto> getById(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false, defaultValue = "false") boolean includeArchived) {
        
        if (orgId == null) { 
            throw new IllegalArgumentException("orgId is required");
        }
        Estimate estimate = estimatesService.getById(id, projectId, orgId, includeArchived);
        
        return ResponseEntity.ok(estimateMapper.toDto(estimate));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER', 'ORG_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<EstimateResponseDto> update(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @RequestBody @Valid UpdateEstimateDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            throw new IllegalArgumentException("orgId is required");
        }
        Estimate estimateUpdates = new Estimate();
        estimateMapper.updateEntity(estimateUpdates, dto);
        
        Estimate updatedEstimate = estimatesService.update(id, projectId, estimateUpdates, orgId);
        
        return ResponseEntity.ok(estimateMapper.toDto(updatedEstimate));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER', 'ORG_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<EstimateResponseDto> archive(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            throw new IllegalArgumentException("orgId is required");
        }
        Estimate archivedEstimate = estimatesService.archive(id, orgId);
        
        return ResponseEntity.ok(estimateMapper.toDto(archivedEstimate));
    }

    @PostMapping("/{id}/unarchive")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER', 'ORG_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<EstimateResponseDto> unarchive(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            throw new IllegalArgumentException("orgId is required");
        }
        // TODO: Implement unarchive method in EstimatesService
        throw new com.mytegroup.api.exception.ServiceUnavailableException("Unarchive not yet implemented");
    }
    
}
