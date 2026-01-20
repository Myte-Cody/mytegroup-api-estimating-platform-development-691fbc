package com.mytegroup.api.controller.projects;

import com.mytegroup.api.dto.projects.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
public class ProjectController {

    // TODO: Inject ProjectService, ProjectMapper

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER', 'PM', 'VIEWER')")
    public ResponseEntity<?> list() {
        // TODO: Implement list projects logic
        return ResponseEntity.ok().build();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER')")
    public ResponseEntity<?> create(@RequestBody @Valid CreateProjectDto dto) {
        // TODO: Implement create project logic
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER', 'PM', 'VIEWER')")
    public ResponseEntity<?> getById(@PathVariable String id) {
        // TODO: Implement get project by id logic
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER')")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody @Valid UpdateProjectDto dto) {
        // TODO: Implement update project logic
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER')")
    public ResponseEntity<?> archive(@PathVariable String id) {
        // TODO: Implement archive project logic
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/unarchive")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER')")
    public ResponseEntity<?> unarchive(@PathVariable String id) {
        // TODO: Implement unarchive project logic
        return ResponseEntity.ok().build();
    }
}

