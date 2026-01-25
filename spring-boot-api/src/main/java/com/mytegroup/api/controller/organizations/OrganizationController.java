package com.mytegroup.api.controller.organizations;

import com.mytegroup.api.dto.organizations.*;
import com.mytegroup.api.dto.response.OrganizationResponseDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.mapper.organizations.OrganizationMapper;
import com.mytegroup.api.mapper.organizations.OrganizationMapper;
import com.mytegroup.api.service.organizations.OrganizationsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationsService organizationsService;
    private final OrganizationMapper organizationMapper;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<OrganizationResponseDto> create(@RequestBody @Valid CreateOrganizationDto dto) {
        // Use mapper to create organization (owner/createdBy can be set later via setOwner)
        Organization org = organizationMapper.toEntity(dto, null, null);
        
        Organization savedOrg = organizationsService.create(org);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(organizationMapper.toDto(savedOrg));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> list(
            @RequestParam(required = false) Boolean includeArchived,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "25") int limit) {
        
        Page<Organization> orgs = organizationsService.list(null, includeArchived, null, null, null, page, limit);
        
        java.util.List<OrganizationResponseDto> dtos = orgs.getContent().stream()
                .map(organizationMapper::toDto)
                .toList();
        
        return ResponseEntity.ok(java.util.Map.of(
                "data", dtos,
                "total", orgs.getTotalElements(),
                "page", page,
                "limit", limit
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_OWNER', 'ADMIN', 'ORG_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<OrganizationResponseDto> getById(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "false") boolean includeArchived) {
        
        Organization org = organizationsService.findById(id);
        if (!includeArchived && org.getArchivedAt() != null) {
            throw new com.mytegroup.api.exception.ResourceNotFoundException("Organization not found");
        }
        
        return ResponseEntity.ok(organizationMapper.toDto(org));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_OWNER', 'ADMIN', 'ORG_ADMIN')")
    public ResponseEntity<OrganizationResponseDto> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateOrganizationDto dto) {
        
        // Create an Organization object with updates using mapper
        Organization orgUpdates = new Organization();
        organizationMapper.updateEntity(orgUpdates, dto);
        
        Organization updatedOrg = organizationsService.update(id, orgUpdates);
        
        return ResponseEntity.ok(organizationMapper.toDto(updatedOrg));
    }

    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<OrganizationResponseDto> archive(@PathVariable Long id) {
        Organization archivedOrg = organizationsService.archive(id);
        
        return ResponseEntity.ok(organizationMapper.toDto(archivedOrg));
    }

    @PatchMapping("/{id}/unarchive")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<OrganizationResponseDto> unarchive(@PathVariable Long id) {
        Organization unarchivedOrg = organizationsService.unarchive(id);
        
        return ResponseEntity.ok(organizationMapper.toDto(unarchivedOrg));
    }

    @PatchMapping("/{id}/datastore")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<OrganizationResponseDto> updateDatastore(
            @PathVariable Long id,
            @RequestBody @Valid UpdateOrganizationDatastoreDto dto) {
        
        // Create Organization object with datastore updates
        Organization datastoreUpdates = new Organization();
        if (dto.type() != null) {
            datastoreUpdates.setDatastoreType(dto.type());
        }
        if (dto.databaseUri() != null) {
            datastoreUpdates.setDatabaseUri(dto.databaseUri());
        }
        if (dto.databaseName() != null) {
            datastoreUpdates.setDatabaseName(dto.databaseName());
        }
        if (dto.dataResidency() != null) {
            datastoreUpdates.setDataResidency(dto.dataResidency());
        }
        if (dto.useDedicatedDb() != null) {
            datastoreUpdates.setUseDedicatedDb(dto.useDedicatedDb());
        }
        
        Organization updatedOrg = organizationsService.updateDatastore(id, datastoreUpdates);
        
        return ResponseEntity.ok(organizationMapper.toDto(updatedOrg));
    }

    @PostMapping("/{id}/legal-hold")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<OrganizationResponseDto> setLegalHold(
            @PathVariable Long id,
            @RequestBody @Valid UpdateOrganizationLegalHoldDto dto) {
        
        Organization updatedOrg = organizationsService.setLegalHold(id, dto.legalHold());
        
        return ResponseEntity.ok(organizationMapper.toDto(updatedOrg));
    }

    @PostMapping("/{id}/pii-stripped")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<OrganizationResponseDto> setPiiStripped(
            @PathVariable Long id,
            @RequestBody @Valid UpdateOrganizationPiiDto dto) {
        
        Organization updatedOrg = organizationsService.setPiiStripped(id, dto.piiStripped());
        
        return ResponseEntity.ok(organizationMapper.toDto(updatedOrg));
    }
}
