package com.mytegroup.api.controller.estimates;

import com.mytegroup.api.dto.estimates.*;
import com.mytegroup.api.entity.projects.Estimate;
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

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER', 'PM', 'VIEWER', 'ORG_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> list(
            @PathVariable Long projectId,
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) Boolean includeArchived) {
        
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        List<Estimate> estimates = estimatesService.listByProject(projectId, orgId, includeArchived);
        
        List<Map<String, Object>> response = estimates.stream()
            .map(this::estimateToMap)
            .toList();
        
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER', 'ORG_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> create(
            @PathVariable Long projectId,
            @RequestBody @Valid CreateEstimateDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        Estimate estimate = new Estimate();
        estimate.setName(dto.name());
        estimate.setDescription(dto.description());
        estimate.setRevision(dto.lineItems() != null && !dto.lineItems().isEmpty() ? 1 : 1);
        estimate.setNotes(dto.notes());
        
        Estimate savedEstimate = estimatesService.create(estimate, projectId, orgId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(estimateToMap(savedEstimate));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER', 'PM', 'VIEWER', 'ORG_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> getById(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false, defaultValue = "false") boolean includeArchived) {
        
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        Estimate estimate = estimatesService.getById(id, projectId, orgId, includeArchived);
        
        return ResponseEntity.ok(estimateToMap(estimate));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER', 'ORG_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> update(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @RequestBody @Valid UpdateEstimateDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        Estimate estimateUpdates = new Estimate();
        estimateUpdates.setName(dto.name());
        estimateUpdates.setDescription(dto.description());
        estimateUpdates.setNotes(dto.notes());
        if (dto.status() != null) {
            estimateUpdates.setStatus(dto.status());
        }
        
        Estimate updatedEstimate = estimatesService.update(id, projectId, estimateUpdates, orgId);
        
        return ResponseEntity.ok(estimateToMap(updatedEstimate));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER', 'ORG_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> archive(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        Estimate archivedEstimate = estimatesService.archive(id, orgId);
        
        return ResponseEntity.ok(estimateToMap(archivedEstimate));
    }

    @PostMapping("/{id}/unarchive")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER', 'ORG_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> unarchive(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        // TODO: Implement unarchive method in EstimatesService
        throw new com.mytegroup.api.exception.ServiceUnavailableException("Unarchive not yet implemented");
    }
    
    // Helper methods
    
    private Map<String, Object> estimateToMap(Estimate estimate) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", estimate.getId());
        map.put("name", estimate.getName());
        map.put("description", estimate.getDescription());
        map.put("revision", estimate.getRevision());
        map.put("notes", estimate.getNotes());
        map.put("projectId", estimate.getProject() != null ? estimate.getProject().getId() : null);
        map.put("createdByUserId", estimate.getCreatedByUser() != null ? estimate.getCreatedByUser().getId() : null);
        map.put("piiStripped", estimate.getPiiStripped());
        map.put("legalHold", estimate.getLegalHold());
        map.put("archivedAt", estimate.getArchivedAt());
        map.put("orgId", estimate.getOrganization() != null ? estimate.getOrganization().getId() : null);
        map.put("createdAt", estimate.getCreatedAt());
        map.put("updatedAt", estimate.getUpdatedAt());
        return map;
    }
}
