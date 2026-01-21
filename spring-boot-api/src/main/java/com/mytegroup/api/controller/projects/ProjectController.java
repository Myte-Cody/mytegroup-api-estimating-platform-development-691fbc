package com.mytegroup.api.controller.projects;

import com.mytegroup.api.dto.projects.*;
import com.mytegroup.api.entity.projects.Project;
import com.mytegroup.api.service.common.ActorContext;
import com.mytegroup.api.service.projects.ProjectsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Projects controller.
 * Endpoints:
 * - GET /projects - List projects (Admin/Manager/OrgOwner/PM/Viewer)
 * - POST /projects - Create project (Admin/Manager/OrgOwner)
 * - GET /projects/:id - Get project (Admin/Manager/OrgOwner/PM/Viewer)
 * - PATCH /projects/:id - Update project (Admin/Manager/OrgOwner)
 * - POST /projects/:id/archive - Archive project (Admin/Manager/OrgOwner)
 * - POST /projects/:id/unarchive - Unarchive project (Admin/Manager/OrgOwner)
 */
@RestController
@RequestMapping("/api/projects")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectsService projectsService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER', 'PM', 'VIEWER', 'ORG_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> list(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean includeArchived,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "25") int limit) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Page<Project> projects = projectsService.list(actor, resolvedOrgId, search, status, includeArchived, page, limit);
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", projects.getContent().stream().map(this::projectToMap).toList());
        response.put("total", projects.getTotalElements());
        response.put("page", page);
        response.put("limit", limit);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER', 'ORG_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> create(
            @RequestBody @Valid CreateProjectDto dto,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Project project = new Project();
        project.setName(dto.getName());
        project.setDescription(dto.getDescription());
        project.setExternalId(dto.getExternalId());
        project.setAddressLine1(dto.getAddressLine1());
        project.setAddressLine2(dto.getAddressLine2());
        project.setCity(dto.getCity());
        project.setState(dto.getState());
        project.setPostalCode(dto.getPostalCode());
        project.setCountry(dto.getCountry());
        project.setLatitude(dto.getLatitude());
        project.setLongitude(dto.getLongitude());
        
        Project savedProject = projectsService.create(project, actor, resolvedOrgId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(projectToMap(savedProject));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER', 'PM', 'VIEWER', 'ORG_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> getById(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false, defaultValue = "false") boolean includeArchived) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Project project = projectsService.getById(id, actor, resolvedOrgId, includeArchived);
        
        return ResponseEntity.ok(projectToMap(project));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER', 'ORG_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateProjectDto dto,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Project projectUpdates = new Project();
        projectUpdates.setName(dto.getName());
        projectUpdates.setDescription(dto.getDescription());
        projectUpdates.setExternalId(dto.getExternalId());
        projectUpdates.setAddressLine1(dto.getAddressLine1());
        projectUpdates.setAddressLine2(dto.getAddressLine2());
        projectUpdates.setCity(dto.getCity());
        projectUpdates.setState(dto.getState());
        projectUpdates.setPostalCode(dto.getPostalCode());
        projectUpdates.setCountry(dto.getCountry());
        projectUpdates.setLatitude(dto.getLatitude());
        projectUpdates.setLongitude(dto.getLongitude());
        
        Project updatedProject = projectsService.update(id, projectUpdates, actor, resolvedOrgId);
        
        return ResponseEntity.ok(projectToMap(updatedProject));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER', 'ORG_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> archive(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Project archivedProject = projectsService.archive(id, actor, resolvedOrgId);
        
        return ResponseEntity.ok(projectToMap(archivedProject));
    }

    @PostMapping("/{id}/unarchive")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER', 'ORG_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> unarchive(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Project unarchivedProject = projectsService.unarchive(id, actor, resolvedOrgId);
        
        return ResponseEntity.ok(projectToMap(unarchivedProject));
    }
    
    // Helper methods
    
    private Map<String, Object> projectToMap(Project project) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", project.getId());
        map.put("name", project.getName());
        map.put("normalizedName", project.getNormalizedName());
        map.put("description", project.getDescription());
        map.put("externalId", project.getExternalId());
        map.put("status", project.getStatus() != null ? project.getStatus().getValue() : null);
        map.put("addressLine1", project.getAddressLine1());
        map.put("addressLine2", project.getAddressLine2());
        map.put("city", project.getCity());
        map.put("state", project.getState());
        map.put("postalCode", project.getPostalCode());
        map.put("country", project.getCountry());
        map.put("latitude", project.getLatitude());
        map.put("longitude", project.getLongitude());
        map.put("piiStripped", project.getPiiStripped());
        map.put("legalHold", project.getLegalHold());
        map.put("archivedAt", project.getArchivedAt());
        map.put("orgId", project.getOrganization() != null ? project.getOrganization().getId() : null);
        map.put("createdAt", project.getCreatedAt());
        map.put("updatedAt", project.getUpdatedAt());
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
