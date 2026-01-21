package com.mytegroup.api.controller.organizations;

import com.mytegroup.api.dto.organizations.*;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.service.common.ActorContext;
import com.mytegroup.api.service.organizations.OrganizationsService;
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
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationsService organizationsService;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> create(@RequestBody @Valid CreateOrganizationDto dto) {
        ActorContext actor = getActorContext();
        
        Organization org = new Organization();
        org.setName(dto.getName());
        org.setPrimaryDomain(dto.getPrimaryDomain());
        
        Organization savedOrg = organizationsService.create(org, actor);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(orgToMap(savedOrg));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> list(
            @RequestParam(required = false) Boolean includeArchived,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "25") int limit) {
        
        ActorContext actor = getActorContext();
        
        Page<Organization> orgs = organizationsService.list(actor, includeArchived, page, limit);
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", orgs.getContent().stream().map(this::orgToMap).toList());
        response.put("total", orgs.getTotalElements());
        response.put("page", page);
        response.put("limit", limit);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_OWNER', 'ADMIN', 'ORG_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> getById(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "false") boolean includeArchived) {
        
        ActorContext actor = getActorContext();
        
        Organization org = organizationsService.getById(id, actor, includeArchived);
        
        return ResponseEntity.ok(orgToMap(org));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_OWNER', 'ADMIN', 'ORG_ADMIN')")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateOrganizationDto dto) {
        
        ActorContext actor = getActorContext();
        
        Organization orgUpdates = new Organization();
        orgUpdates.setName(dto.getName());
        orgUpdates.setPrimaryDomain(dto.getPrimaryDomain());
        
        Organization updatedOrg = organizationsService.update(id, orgUpdates, actor);
        
        return ResponseEntity.ok(orgToMap(updatedOrg));
    }

    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> archive(@PathVariable Long id) {
        ActorContext actor = getActorContext();
        
        Organization archivedOrg = organizationsService.archive(id, actor);
        
        return ResponseEntity.ok(orgToMap(archivedOrg));
    }

    @PatchMapping("/{id}/unarchive")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> unarchive(@PathVariable Long id) {
        ActorContext actor = getActorContext();
        
        Organization unarchivedOrg = organizationsService.unarchive(id, actor);
        
        return ResponseEntity.ok(orgToMap(unarchivedOrg));
    }

    @PatchMapping("/{id}/datastore")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> updateDatastore(
            @PathVariable Long id,
            @RequestBody @Valid UpdateOrganizationDatastoreDto dto) {
        
        ActorContext actor = getActorContext();
        
        Organization updatedOrg = organizationsService.updateDatastore(id, dto.getDatastoreType(), dto.getDatastoreConfig(), actor);
        
        return ResponseEntity.ok(orgToMap(updatedOrg));
    }

    @PostMapping("/{id}/legal-hold")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> setLegalHold(
            @PathVariable Long id,
            @RequestBody @Valid UpdateOrganizationLegalHoldDto dto) {
        
        ActorContext actor = getActorContext();
        
        Organization updatedOrg = organizationsService.setLegalHold(id, dto.getLegalHold(), actor);
        
        return ResponseEntity.ok(orgToMap(updatedOrg));
    }

    @PostMapping("/{id}/pii-stripped")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> setPiiStripped(
            @PathVariable Long id,
            @RequestBody @Valid UpdateOrganizationPiiDto dto) {
        
        ActorContext actor = getActorContext();
        
        Organization updatedOrg = organizationsService.stripPii(id, actor);
        
        return ResponseEntity.ok(orgToMap(updatedOrg));
    }
    
    // Helper methods
    
    private Map<String, Object> orgToMap(Organization org) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", org.getId());
        map.put("name", org.getName());
        map.put("primaryDomain", org.getPrimaryDomain());
        map.put("datastoreType", org.getDatastoreType() != null ? org.getDatastoreType().getValue() : null);
        map.put("datastoreConfig", org.getDatastoreConfig());
        map.put("ownerId", org.getOwner() != null ? org.getOwner().getId() : null);
        map.put("piiStripped", org.getPiiStripped());
        map.put("legalHold", org.getLegalHold());
        map.put("archivedAt", org.getArchivedAt());
        map.put("createdAt", org.getCreatedAt());
        map.put("updatedAt", org.getUpdatedAt());
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
