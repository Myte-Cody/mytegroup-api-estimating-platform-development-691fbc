package com.mytegroup.api.controller.projects;

import com.mytegroup.api.dto.projects.*;
import com.mytegroup.api.entity.projects.Project;
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
        
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        Page<Project> projects = projectsService.list(orgId, search, status, includeArchived, page, limit);
        
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
        
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        Project project = new Project();
        project.setName(dto.name());
        project.setDescription(dto.description());
        if (dto.status() != null) {
            project.setStatus(dto.status());
        }
        if (dto.projectCode() != null) {
            project.setProjectCode(dto.projectCode());
        }
        if (dto.location() != null) {
            project.setLocation(dto.location());
        }
        if (dto.officeId() != null) {
            // Office will be set by service
        }
        
        Project savedProject = projectsService.create(project, orgId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(projectToMap(savedProject));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER', 'PM', 'VIEWER', 'ORG_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> getById(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false, defaultValue = "false") boolean includeArchived) {
        
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        Project project = projectsService.getById(id, orgId, includeArchived);
        
        return ResponseEntity.ok(projectToMap(project));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER', 'ORG_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateProjectDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        Project projectUpdates = new Project();
        projectUpdates.setName(dto.name());
        projectUpdates.setDescription(dto.description());
        if (dto.status() != null) {
            projectUpdates.setStatus(dto.status());
        }
        if (dto.projectCode() != null) {
            projectUpdates.setProjectCode(dto.projectCode());
        }
        if (dto.location() != null) {
            projectUpdates.setLocation(dto.location());
        }
        
        Project updatedProject = projectsService.update(id, projectUpdates, orgId);
        
        return ResponseEntity.ok(projectToMap(updatedProject));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER', 'ORG_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> archive(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        Project archivedProject = projectsService.archive(id, orgId);
        
        return ResponseEntity.ok(projectToMap(archivedProject));
    }

    @PostMapping("/{id}/unarchive")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER', 'ORG_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> unarchive(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        Project unarchivedProject = projectsService.unarchive(id, orgId);
        
        return ResponseEntity.ok(projectToMap(unarchivedProject));
    }
    
    // Helper methods
    
    private Map<String, Object> projectToMap(Project project) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", project.getId());
        map.put("name", project.getName());
        map.put("description", project.getDescription());
        map.put("projectCode", project.getProjectCode());
        map.put("status", project.getStatus());
        map.put("location", project.getLocation());
        map.put("officeId", project.getOffice() != null ? project.getOffice().getId() : null);
        map.put("piiStripped", project.getPiiStripped());
        map.put("legalHold", project.getLegalHold());
        map.put("archivedAt", project.getArchivedAt());
        map.put("orgId", project.getOrganization() != null ? project.getOrganization().getId() : null);
        map.put("createdAt", project.getCreatedAt());
        map.put("updatedAt", project.getUpdatedAt());
        return map;
    }
}
