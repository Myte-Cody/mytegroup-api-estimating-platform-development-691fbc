package com.mytegroup.api.controller.estimates;

import com.mytegroup.api.dto.estimates.*;
import com.mytegroup.api.entity.projects.Estimate;
import com.mytegroup.api.service.common.ActorContext;
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
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        List<Estimate> estimates = estimatesService.listByProject(projectId, actor, resolvedOrgId, includeArchived);
        
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
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Estimate estimate = new Estimate();
        estimate.setName(dto.getName());
        estimate.setDescription(dto.getDescription());
        estimate.setExternalId(dto.getExternalId());
        estimate.setRevisionNumber(dto.getRevisionNumber());
        estimate.setNotes(dto.getNotes());
        
        Estimate savedEstimate = estimatesService.create(estimate, projectId, actor, resolvedOrgId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(estimateToMap(savedEstimate));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER', 'PM', 'VIEWER', 'ORG_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> getById(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false, defaultValue = "false") boolean includeArchived) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Estimate estimate = estimatesService.getById(id, projectId, actor, resolvedOrgId, includeArchived);
        
        return ResponseEntity.ok(estimateToMap(estimate));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER', 'ORG_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> update(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @RequestBody @Valid UpdateEstimateDto dto,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Estimate estimateUpdates = new Estimate();
        estimateUpdates.setName(dto.getName());
        estimateUpdates.setDescription(dto.getDescription());
        estimateUpdates.setExternalId(dto.getExternalId());
        estimateUpdates.setRevisionNumber(dto.getRevisionNumber());
        estimateUpdates.setNotes(dto.getNotes());
        
        Estimate updatedEstimate = estimatesService.update(id, projectId, estimateUpdates, actor, resolvedOrgId);
        
        return ResponseEntity.ok(estimateToMap(updatedEstimate));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER', 'ORG_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> archive(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Estimate archivedEstimate = estimatesService.archive(id, projectId, actor, resolvedOrgId);
        
        return ResponseEntity.ok(estimateToMap(archivedEstimate));
    }

    @PostMapping("/{id}/unarchive")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER', 'ORG_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> unarchive(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Estimate unarchivedEstimate = estimatesService.unarchive(id, projectId, actor, resolvedOrgId);
        
        return ResponseEntity.ok(estimateToMap(unarchivedEstimate));
    }
    
    // Helper methods
    
    private Map<String, Object> estimateToMap(Estimate estimate) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", estimate.getId());
        map.put("name", estimate.getName());
        map.put("description", estimate.getDescription());
        map.put("externalId", estimate.getExternalId());
        map.put("revisionNumber", estimate.getRevisionNumber());
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
