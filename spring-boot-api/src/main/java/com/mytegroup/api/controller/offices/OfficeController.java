package com.mytegroup.api.controller.offices;

import com.mytegroup.api.dto.offices.*;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.organization.Office;
import com.mytegroup.api.mapper.offices.OfficeMapper;
import com.mytegroup.api.service.common.ServiceAuthorizationHelper;
import com.mytegroup.api.service.offices.OfficesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/offices")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class OfficeController {

    private final OfficesService officesService;
    private final OfficeMapper officeMapper;
    private final ServiceAuthorizationHelper authHelper;

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> list(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) Boolean includeArchived,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "25") int limit) {
        
        if (orgId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required"));
        }
        
        List<Office> offices = officesService.list(orgId, includeArchived != null && includeArchived);
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", offices.stream().map(this::officeToMap).toList());
        response.put("total", offices.size());
        response.put("page", page);
        response.put("limit", limit);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> create(
            @RequestBody @Valid CreateOfficeDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required"));
        }
        
        // Get organization for mapper
        Organization organization = authHelper.validateOrg(orgId);
        Office parent = null;
        if (dto.parentOrgLocationId() != null) {
            // Parent will be validated by service
        }
        
        Office office = officeMapper.toEntity(dto, organization, parent);
        
        Office savedOffice = officesService.create(office, orgId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(officeToMap(savedOffice));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> getById(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false, defaultValue = "false") boolean includeArchived) {
        
        Office office = officesService.getById(id, orgId, includeArchived);
        
        return ResponseEntity.ok(officeToMap(office));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateOfficeDto dto,
            @RequestParam(required = false) String orgId) {
        
        // Create an Office object with updates using mapper
        Office officeUpdates = new Office();
        Office parent = null;
        if (dto.parentOrgLocationId() != null) {
            // Parent will be handled by service
        }
        officeMapper.updateEntity(officeUpdates, dto, parent);
        
        Office updatedOffice = officesService.update(id, officeUpdates, orgId);
        
        return ResponseEntity.ok(officeToMap(updatedOffice));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> archive(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        Office archivedOffice = officesService.archive(id, orgId);
        
        return ResponseEntity.ok(officeToMap(archivedOffice));
    }

    @PostMapping("/{id}/unarchive")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> unarchive(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        Office unarchivedOffice = officesService.unarchive(id, orgId);
        
        return ResponseEntity.ok(officeToMap(unarchivedOffice));
    }
    
    // Helper methods
    
    private Map<String, Object> officeToMap(Office office) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", office.getId());
        map.put("name", office.getName());
        map.put("name", office.getName());
        map.put("description", office.getDescription());
        map.put("timezone", office.getTimezone());
        map.put("orgLocationTypeKey", office.getOrgLocationTypeKey());
        map.put("address", office.getAddress());
        map.put("tagKeys", office.getTagKeys());
        map.put("parentId", office.getParent() != null ? office.getParent().getId() : null);
        map.put("sortOrder", office.getSortOrder());
        map.put("piiStripped", office.getPiiStripped());
        map.put("legalHold", office.getLegalHold());
        map.put("archivedAt", office.getArchivedAt());
        map.put("orgId", office.getOrganization() != null ? office.getOrganization().getId() : null);
        map.put("createdAt", office.getCreatedAt());
        map.put("updatedAt", office.getUpdatedAt());
        return map;
    }
    
}
