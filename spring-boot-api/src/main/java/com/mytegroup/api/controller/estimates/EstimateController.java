package com.mytegroup.api.controller.estimates;

import com.mytegroup.api.dto.estimates.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
public class EstimateController {

    // TODO: Inject EstimateService, EstimateMapper

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER', 'PM', 'VIEWER')")
    public ResponseEntity<?> list(@PathVariable String projectId) {
        // TODO: Implement list estimates logic
        return ResponseEntity.ok().build();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER')")
    public ResponseEntity<?> create(@PathVariable String projectId, @RequestBody @Valid CreateEstimateDto dto) {
        // TODO: Implement create estimate logic
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER', 'PM', 'VIEWER')")
    public ResponseEntity<?> getById(@PathVariable String projectId, @PathVariable String id) {
        // TODO: Implement get estimate by id logic
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER')")
    public ResponseEntity<?> update(@PathVariable String projectId, @PathVariable String id, @RequestBody @Valid UpdateEstimateDto dto) {
        // TODO: Implement update estimate logic
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER')")
    public ResponseEntity<?> archive(@PathVariable String projectId, @PathVariable String id) {
        // TODO: Implement archive estimate logic
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/unarchive")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ORG_OWNER')")
    public ResponseEntity<?> unarchive(@PathVariable String projectId, @PathVariable String id) {
        // TODO: Implement unarchive estimate logic
        return ResponseEntity.ok().build();
    }
}

