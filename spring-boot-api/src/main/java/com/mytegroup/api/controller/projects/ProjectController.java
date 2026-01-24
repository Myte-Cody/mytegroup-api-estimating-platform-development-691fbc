package com.mytegroup.api.controller.projects;

import com.mytegroup.api.dto.projects.*;
import com.mytegroup.api.dto.response.ProjectResponseDto;
import com.mytegroup.api.dto.response.PaginatedResponseDto;
import com.mytegroup.api.entity.projects.Project;
import com.mytegroup.api.mapper.projects.ProjectMapper;
import com.mytegroup.api.mapper.response.ProjectResponseMapper;
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
    private final ProjectMapper projectMapper;
    private final ProjectResponseMapper projectResponseMapper;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER', 'PM', 'VIEWER', 'ORG_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<PaginatedResponseDto<ProjectResponseDto>> list(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean includeArchived,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "25") int limit) {
        
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(PaginatedResponseDto.<ProjectResponseDto>builder()
                    .data(java.util.List.of())
                    .total(0)
                    .page(page)
                    .limit(limit)
                    .build());
        }
        Page<Project> projects = projectsService.list(orgId, search, status, includeArchived, page, limit);
        
        return ResponseEntity.ok(PaginatedResponseDto.<ProjectResponseDto>builder()
                .data(projects.getContent().stream().map(projectResponseMapper::toDto).toList())
                .total(projects.getTotalElements())
                .page(page)
                .limit(limit)
                .build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER', 'ORG_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<ProjectResponseDto> create(
            @RequestBody @Valid CreateProjectDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            throw new IllegalArgumentException("orgId is required");
        }
        Project project = projectMapper.toEntity(dto, null, null);
        
        Project savedProject = projectsService.create(project, orgId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(projectResponseMapper.toDto(savedProject));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER', 'PM', 'VIEWER', 'ORG_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<ProjectResponseDto> getById(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false, defaultValue = "false") boolean includeArchived) {
        
        if (orgId == null) { 
            throw new IllegalArgumentException("orgId is required");
        }
        Project project = projectsService.getById(id, orgId, includeArchived);
        
        return ResponseEntity.ok(projectResponseMapper.toDto(project));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER', 'ORG_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<ProjectResponseDto> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateProjectDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            throw new IllegalArgumentException("orgId is required");
        }
        Project projectUpdates = new Project();
        projectMapper.updateEntity(projectUpdates, dto, null);
        
        Project updatedProject = projectsService.update(id, projectUpdates, orgId);
        
        return ResponseEntity.ok(projectResponseMapper.toDto(updatedProject));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER', 'ORG_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<ProjectResponseDto> archive(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            throw new IllegalArgumentException("orgId is required");
        }
        Project archivedProject = projectsService.archive(id, orgId);
        
        return ResponseEntity.ok(projectResponseMapper.toDto(archivedProject));
    }

    @PostMapping("/{id}/unarchive")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER', 'ORG_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<ProjectResponseDto> unarchive(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            throw new IllegalArgumentException("orgId is required");
        }
        Project unarchivedProject = projectsService.unarchive(id, orgId);
        
        return ResponseEntity.ok(projectResponseMapper.toDto(unarchivedProject));
    }
    
}
