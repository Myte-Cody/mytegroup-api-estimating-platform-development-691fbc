package com.mytegroup.api.controller.organizations;

import com.mytegroup.api.dto.organizations.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Organizations controller.
 * Endpoints:
 * - POST /organizations - Create org (SuperAdmin)
 * - GET /organizations - List orgs (SuperAdmin/PlatformAdmin)
 * - GET /organizations/:id - Get org (SuperAdmin/OrgOwner/Admin)
 * - PATCH /organizations/:id - Update org (SuperAdmin/OrgOwner/Admin)
 * - PATCH /organizations/:id/archive - Archive org (SuperAdmin)
 * - PATCH /organizations/:id/unarchive - Unarchive org (SuperAdmin)
 * - PATCH /organizations/:id/datastore - Update datastore (SuperAdmin)
 * - POST /organizations/:id/legal-hold - Set legal hold (SuperAdmin)
 * - POST /organizations/:id/pii-stripped - Set PII stripped (SuperAdmin)
 */
@RestController
@RequestMapping("/api/organizations")
@PreAuthorize("isAuthenticated()")
public class OrganizationController {

    // TODO: Inject OrganizationService, OrganizationMapper

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> create(@RequestBody @Valid CreateOrganizationDto dto) {
        // TODO: Implement create organization logic
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> list(@ModelAttribute ListOrganizationsDto query) {
        // TODO: Implement list organizations logic
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_OWNER', 'ADMIN')")
    public ResponseEntity<?> getById(@PathVariable String id) {
        // TODO: Implement get organization by id logic
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_OWNER', 'ADMIN')")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody @Valid UpdateOrganizationDto dto) {
        // TODO: Implement update organization logic
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> archive(@PathVariable String id) {
        // TODO: Implement archive organization logic
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/unarchive")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> unarchive(@PathVariable String id) {
        // TODO: Implement unarchive organization logic
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/datastore")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> updateDatastore(@PathVariable String id, @RequestBody @Valid UpdateOrganizationDatastoreDto dto) {
        // TODO: Implement update datastore logic
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/legal-hold")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> setLegalHold(@PathVariable String id, @RequestBody @Valid UpdateOrganizationLegalHoldDto dto) {
        // TODO: Implement set legal hold logic
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/pii-stripped")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> setPiiStripped(@PathVariable String id, @RequestBody @Valid UpdateOrganizationPiiDto dto) {
        // TODO: Implement set PII stripped logic
        return ResponseEntity.ok().build();
    }
}

