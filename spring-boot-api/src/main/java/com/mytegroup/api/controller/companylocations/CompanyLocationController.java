package com.mytegroup.api.controller.companylocations;

import com.mytegroup.api.dto.companylocations.*;
import com.mytegroup.api.entity.companies.Company;
import com.mytegroup.api.entity.companies.CompanyLocation;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.mapper.companylocations.CompanyLocationMapper;
import com.mytegroup.api.service.common.ServiceAuthorizationHelper;
import com.mytegroup.api.service.companylocations.CompanyLocationsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/company-locations")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class CompanyLocationController {

    private final CompanyLocationsService companyLocationsService;
    private final CompanyLocationMapper companyLocationMapper;
    private final ServiceAuthorizationHelper authHelper;

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> list(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Boolean includeArchived,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "25") int limit) {
        
        if (orgId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required"));
        }
        
        Page<CompanyLocation> locations = companyLocationsService.list(orgId, companyId, null, null, includeArchived, page, limit);
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", locations.getContent().stream().map(this::locationToMap).toList());
        response.put("total", locations.getTotalElements());
        response.put("page", page);
        response.put("limit", limit);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> create(
            @RequestBody @Valid CreateCompanyLocationDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required"));
        }
        
        // Get organization and company for mapper
        Organization organization = authHelper.validateOrg(orgId);
        Company company = new Company();
        company.setId(Long.parseLong(dto.companyId())); // Service will validate this
        
        CompanyLocation location = companyLocationMapper.toEntity(dto, organization, company);
        
        CompanyLocation savedLocation = companyLocationsService.create(location, orgId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(locationToMap(savedLocation));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> getById(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false, defaultValue = "false") boolean includeArchived) {
        
        CompanyLocation location = companyLocationsService.getById(id, orgId, includeArchived);
        
        return ResponseEntity.ok(locationToMap(location));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateCompanyLocationDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required"));
        }
        
        // Create a CompanyLocation object with updates using mapper
        CompanyLocation locationUpdates = new CompanyLocation();
        companyLocationMapper.updateEntity(locationUpdates, dto);
        
        CompanyLocation updatedLocation = companyLocationsService.update(id, locationUpdates, orgId);
        
        return ResponseEntity.ok(locationToMap(updatedLocation));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> archive(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required"));
        }
        
        CompanyLocation archivedLocation = companyLocationsService.archive(id, orgId);
        
        return ResponseEntity.ok(locationToMap(archivedLocation));
    }

    @PostMapping("/{id}/unarchive")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> unarchive(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required"));
        }
        
        CompanyLocation unarchivedLocation = companyLocationsService.unarchive(id, orgId);
        
        return ResponseEntity.ok(locationToMap(unarchivedLocation));
    }
    
    // Helper methods
    
    private Map<String, Object> locationToMap(CompanyLocation location) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", location.getId());
        map.put("name", location.getName());
        map.put("externalId", location.getExternalId());
        map.put("addressLine1", location.getAddressLine1());
        map.put("addressLine2", location.getAddressLine2());
        map.put("city", location.getCity());
        map.put("region", location.getRegion());
        map.put("postal", location.getPostal());
        map.put("country", location.getCountry());
        map.put("phone", location.getPhone());
        map.put("email", location.getEmail());
        map.put("notes", location.getNotes());
        map.put("companyId", location.getCompany() != null ? location.getCompany().getId() : null);
        map.put("archivedAt", location.getArchivedAt());
        map.put("createdAt", location.getCreatedAt());
        map.put("updatedAt", location.getUpdatedAt());
        return map;
    }
    
}
